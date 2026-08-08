export type AlgorithmDifficulty = 'easy' | 'medium' | 'hard'
export type AlgorithmSessionStatus = 'IN_PROGRESS' | 'FINISHED' | 'ABANDONED'
export type AlgorithmTurnState = 'IDLE' | 'PROCESSING' | 'RETRYABLE_ERROR'
export type AlgorithmInputModality = 'TEXT' | 'VOICE_TRANSCRIPT'

export interface AlgorithmProblem {
  id: number
  code: string
  title: string
  statement: string
  difficulty: AlgorithmDifficulty
  tags: string[]
  constraints: string[]
}

export interface AlgorithmTurn {
  id: number
  sequence: number
  role: 'INTERVIEWER' | 'CANDIDATE'
  stage: string
  content: string
  inputModality: AlgorithmInputModality
  parentTurnId: number | null
  createdAt: string
}

export interface AlgorithmSession {
  sessionId: number
  status: AlgorithmSessionStatus
  currentStage: string
  version: number
  turnState: AlgorithmTurnState
  problem: AlgorithmProblem
  turns: AlgorithmTurn[]
}

export interface AlgorithmDimension {
  dimension: string
  status: 'ASSESSED' | 'NOT_ASSESSED'
  score: number | null
}

export interface AlgorithmConclusion {
  text: string
  candidateTurnId: number
  evaluationId: number
  candidateEvidence: string
}

export interface AlgorithmRoundReview {
  sequence: number
  stage: string
  candidateAnswer: string
  scores: Record<string, number | null>
  strengths: string[]
  gaps: string[]
  evidence: string[]
  candidateTurnId: number
  evaluationId: number
}

export interface AlgorithmReport {
  schemaVersion: number
  sessionId: number
  completionStatus: 'COMPLETE' | 'PARTIAL'
  overallScore: number | null
  coverage: number
  dimensions: AlgorithmDimension[]
  strengths: AlgorithmConclusion[]
  gaps: AlgorithmConclusion[]
  recommendations: string[]
  rounds: AlgorithmRoundReview[]
  generatedAt: string
}
