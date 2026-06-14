# BPMN Blockchain Analyzer

## Project Overview

This project analyzes BPMN process models and produces blockchain suitability recommendations for each BPMN node.
It evaluates nodes based on Wust-style shared-ledger criteria and a soft scoring model.

## What was fixed

### 1. Parser improvements
- The BPMN parser now recognizes additional BPMN node types:
  - `bpmn:sendTask`
  - `bpmn:receiveTask`
- It continues to parse:
  - `bpmn:userTask`
  - `bpmn:serviceTask`
  - `bpmn:manualTask`
  - `bpmn:exclusiveGateway`
  - `bpmn:parallelGateway`
  - `bpmn:startEvent`
  - `bpmn:endEvent`

### 2. Better BPMN flow inference
- Added support for `bpmn:messageFlow` relationships.
- Message-flow-connected nodes now infer:
  - `externalDataFlow = true`
  - `crossOrganizationFlow = true`
- Participant lanes (`bpmn:lane` / `bpmn:flowNodeRef`) are parsed and used to detect cross-organization flows when connected nodes belong to different lanes.

### 3. Node-level determination of the Wust gate
- Fixed the hard-gate evaluation so each node is judged by its own flags instead of graph-level context.
- This avoids incorrect cases where one external node made all nodes pass the shared-ledger gate.

### 4. Context heuristics and soft scoring
- Node name heuristics now detect common blockchain-relevant signals:
  - customer, supplier, order, delivery, shipment, payment, invoice, billing, message, send, receive
- Soft scoring still uses equal AHP weights for all criteria.
- Current thresholds are:
  - `NOT_SUITABLE` if score < 40
  - `PARTIALLY_SUITABLE` if 40 ≤ score < 70
  - `BLOCKCHAIN_SUITABLE` if score ≥ 70

## Key files

- `src/main/java/com/blockchain/analyzer/parser/BPMNParser.java`
  - BPMN XML parsing and flag inference logic
- `src/main/java/com/blockchain/analyzer/blockchain/scoring/BlockchainScoringEngine.java`
  - Wust gate logic and scoring model
- `output-from-bpmn.json`
  - Example analyzer output for the provided BPMN file

## How to run

On Windows:

```powershell
cd d:\Thopaz\Kuliah\KP\BPMN\bpmn-blockchain-analyzer
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

The API controller accepts BPMN XML upload and returns JSON recommendations.

## Current recommendation status

- The analyzer now generates more accurate gate decisions by relying on node-level context.
- `output-from-bpmn.json` shows the current results, but the model still depends on parser heuristics.

## Next recommended improvements

1. Improve BPMN semantics support
   - Parse `bpmn:participant`, `bpmn:collaboration`, `bpmn:dataObject`, and data associations.
   - Add better participant/participant boundary detection.

2. Strengthen graph-level context only where appropriate
   - Use graph context for summarizing the model, not for node-level gate passes.

3. Tune the scoring model
   - If you want domain-specific behavior, adjust AHP weights and thresholds.
   - Example: increase weight for `CROSS_ORGANIZATION_FLOW` and `EXTERNAL_DATA_FLOW`.

4. Add regression tests
   - Create BPMN tests for message flow, participant lanes, and expected suitability outputs.

## Notes

- The fixed logic is now based on the actual BPMN node qualities inferred from task names and message flows.
- The current `AHP_PAIRWISE` matrix uses equal weights; that is acceptable for an initial model.
- Final validation should use your actual BPMN XML and confirm whether node suitability matches your business expectations.
