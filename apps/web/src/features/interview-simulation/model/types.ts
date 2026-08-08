import type { AlgorithmProblem, AlgorithmSession } from '@/features/algorithm-practice/model/types'
import type { ProjectInterviewSession, ProjectInterviewReport, InputModality } from '@/features/project-deep-dive/model/types'
import type { InterviewReport, InterviewSession } from '@/api/modules/interview'

export type SimulationStage = 'PROJECT' | 'KNOWLEDGE' | 'ALGORITHM' | 'FINISHED'

export interface SimulationProjectOption {
  id: number
  name: string
  summary: string
  techStack: string[]
}

export interface SimulationOptions {
  projects: SimulationProjectOption[]
  algorithmProblems: AlgorithmProblem[]
  knowledgeModules: string[]
  difficulties: string[]
}

export interface SimulationStageState {
  stageType: Exclude<SimulationStage, 'FINISHED'>
  sequence: number
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED'
  businessSessionId: number
}

export interface SimulationSession {
  simulationId: number
  status: 'IN_PROGRESS' | 'FINISHED'
  currentStage: SimulationStage
  version: number
  stages: SimulationStageState[]
  stageData: ProjectInterviewSession | InterviewSession | AlgorithmSession | null
}

export interface CreateSimulationPayload {
  clientRequestId: string
  projectProfileId: number
  algorithmProblemId: number
  knowledgeModule: string
  difficulty: string
}

export interface SubmitSimulationPayload {
  clientTurnId: string
  questionTurnId: number | null
  expectedChildVersion: number | null
  content: string
  inputModality: InputModality
}

export interface SimulationStageReport {
  stageType: Exclude<SimulationStage, 'FINISHED'>
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED'
  report: ProjectInterviewReport | InterviewReport | import('@/features/algorithm-practice/model/types').AlgorithmReport | null
}

export interface SimulationReport {
  schemaVersion: number
  simulationId: number
  completionStatus: 'COMPLETE' | 'PARTIAL'
  stages: SimulationStageReport[]
  recommendations: string[]
  generatedAt: string
}
