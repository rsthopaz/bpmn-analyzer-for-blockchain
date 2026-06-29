# BPMN Blockchain Analyzer

## What this project does

This application reads a process diagram file, converts it into a simple workflow graph, and evaluates whether each task or decision node could be a good fit for blockchain.

It is not a full BPMN engine. Instead, it uses patterns from the diagram and node descriptions to decide whether a node looks like:

- shared data between multiple parties,
- a cross-organization workflow,
- a private or sensitive data transfer,
- or a transaction/approval step.

The output is a JSON list of recommendations with:

- `suitability` (NOT_SUITABLE, PARTIALLY_SUITABLE, or BLOCKCHAIN_SUITABLE)
- `score`
- `recommendedBlockchain`
- `executionMode`
- reasons why the node passed or failed the gate

## How the code works

### 1. File parsing

The app accepts uploaded XML files through the `/api/blockchain/analyze` endpoint.
The uploaded file is read as a string and passed to `BPMNParser.parse(...)`.

### 2. BPMN vs XPDL detection

`BPMNParser` checks the XML root element:

- if the root is `Package`, the file is treated as XPDL,
- otherwise the file is treated as BPMN XML.

For XPDL, the parser currently extracts:

- `Activity` elements as workflow nodes,
- `Transition` elements as sequence flows,
- `Pool` → `WorkflowProcess` links to capture participant names,
- whether a `WorkflowProcess` uses `DataStoreReference` or `DataAssociation` to mark external/cross-organization flow.

For BPMN XML, the parser extracts:

- user task, service task, manual task, send task, receive task,
- exclusive and parallel gateways,
- start and end events,
- message flows,
- sequence flows,
- lanes and flow node references to detect different participants.

### 3. Node enrichment

Each node is enriched by `BPMNParser.enrichNode(...)`.
That method examines the node name and type and sets boolean flags such as:

- `approvalTask`
- `financialTask`
- `externalInteraction`
- `externalDataFlow`
- `crossOrganizationFlow`

For example, names containing `send`, `receive`, `order`, `payment`, `patient`, or `referral` can make a node look like it crosses organizations or involves external interaction.

### 4. Flow inference

After parsing, `inferFlowFlags(...)` updates nodes based on connections:

- if a node has a `messageFlow` connection, it is marked as external data flow and cross-organization flow,
- if two nodes connected by a message flow belong to different participant lanes, both are marked as cross-organization.

### 5. Scoring and suitability

`BlockchainScoringEngine.evaluate(...)` applies a two-stage decision:

#### WUST hard gate

The node must satisfy:

- shared ledger requirement: `externalDataFlow` or `crossOrganizationFlow`,
- multiple writers: `crossOrganizationFlow`,
- untrusted stakeholders or private data: `externalInteraction` or sensitive node name.

If the node fails this gate, it is marked `NOT_SUITABLE` and the recommended blockchain is `Not applicable`.

#### Soft scoring

If the gate passes, the engine computes a score from criteria including:

- financial process,
- external interaction,
- approval workflow,
- service automation,
- decision workflow,
- cross-organization flow,
- external data flow.

Then suitability is assigned as:

- `BLOCKCHAIN_SUITABLE` if score ≥ 70,
- `PARTIALLY_SUITABLE` if 40 ≤ score < 70,
- `NOT_SUITABLE` otherwise.

### 6. Recommendation output

The controller returns JSON with one recommendation per graph node.
Example fields include:

- `serviceId`
- `serviceName`
- `score`
- `suitability`
- `recommendedBlockchain`
- `executionMode`
- `gateReasons`
- `reasons`

## Why a diagram may still show `NOT_SUITABLE`

If your process is not detected as having:

- cross-organization flow,
- external data flow,
- external stakeholders, or
- private/sensitive data,

then the WUST gate will fail and the node becomes `NOT_SUITABLE`.

The current parser is heuristic-based, so it may miss cases that are clearly business-relevant but do not use the exact keywords or XPDL structure it expects.

## How to use it

### Run locally

```powershell
cd d:\Thopaz\Kuliah\KP\BPMN\bpmn-blockchain-analyzer
./mvnw.cmd clean package
./mvnw.cmd spring-boot:run
```

### Upload a file

Use the endpoint:

```
POST /api/blockchain/analyze
Content-Type: multipart/form-data
file=@your-diagram.bpmn
```

The file can be a BPMN XML file or an XPDL file.

### Local test example with curl

```powershell
curl -F "file=@\"d:\Thopaz\Kuliah\KP\BPMN\bpmn-blockchain-analyzer\Diagram 1.xpdl\"" http://localhost:8080/api/blockchain/analyze
```

## What changed recently

### XPDL support

- The parser now detects XPDL's root `Package` element.
- It pulls nodes from `Activity` and edges from `Transition`.
- It uses pool/process linkage to infer participant names and external data flow.

### Healthcare/referral heuristics

- Node names containing `patient`, `referral`, `hospital`, `clinic`, `medical`, or `registration` are now treated as sensitive or external interaction signals.
- This makes the WUST gate more likely to recognize referral scenarios as potentially blockchain-relevant.

## What to improve next

If you want better accuracy, the next steps are:

1. Add proper XPDL lane/Activity/DataAssociation parsing,
2. Map actual data stores and associations to the nodes that use them,
3. Add more test cases for real BPMN/XPDL diagrams,
4. Tune the scoring weights or thresholds for your business domain.
