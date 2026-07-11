export type InputModality = 'TEXT' | 'VOICE_TRANSCRIPT'

export type ProjectAnalysisStatus = 'DRAFT' | 'ANALYZING' | 'REVIEW_REQUIRED' | 'READY' | 'FAILED'

export type ProjectClaimType =
  | 'RESPONSIBILITY'
  | 'TECHNICAL_CHOICE'
  | 'PERFORMANCE_IMPROVEMENT'
  | 'ARCHITECTURE_DESIGN'
  | 'INCIDENT_HANDLING'
  | 'BUSINESS_RESULT'

export interface ProjectClaim {
  claimId: number
  claimType: ProjectClaimType
  statement: string
  sourceFragment: string
  relatedTechnologies: string[]
  confirmed: boolean
}

export interface ProjectProfile {
  profileId: number
  sanitizedDescription: string
  projectName: string | null
  summary: string | null
  techStack: string[]
  responsibilities: string[]
  metrics: string[]
  architecture: string[]
  uncertainties: string[]
  analysisStatus: ProjectAnalysisStatus
  version: number
  claims: ProjectClaim[]
  createTime: string
  updateTime: string
}

export interface CreateProjectProfileResult {
  profileId: number
  accessToken: string
  analysisStatus: ProjectAnalysisStatus
  sanitizedDescription: string
  version: number
}

export interface PatchProjectClaim {
  claimId: number
  claimType: ProjectClaimType
  statement: string
  sourceFragment: string
  relatedTechnologies: string[]
}

export interface PatchProjectProfilePayload {
  version: number
  projectName: string
  summary: string
  techStack: string[]
  responsibilities: string[]
  metrics: string[]
  architecture: string[]
  uncertainties: string[]
  claims: PatchProjectClaim[]
}

export interface ProjectReviewForm {
  projectName: string
  summary: string
  techStack: string[]
  responsibilities: string[]
  metrics: string[]
  architecture: string[]
  uncertainties: string[]
  claims: PatchProjectClaim[]
}

export type InterviewRole = 'INTERVIEWER' | 'CANDIDATE' | 'SYSTEM'
export type InterviewTurnType = 'OPENING' | 'MAIN' | 'FOLLOW_UP' | 'ANSWER' | 'TRANSITION' | 'CLOSING'
export type InterviewSessionStatus = 'PREPARING' | 'IN_PROGRESS' | 'FINISHING' | 'FINISHED' | 'CANCELLED'

export interface PublicInterviewTurn {
  turnId: number
  sequenceNo: number
  role: InterviewRole
  turnType: InterviewTurnType
  content: string
  inputModality: InputModality
  startedAt: string | null
  endedAt: string | null
  createTime: string
}

export interface ProjectInterviewSession {
  sessionId: number
  mode: 'PROJECT_DEEP_DIVE'
  status: InterviewSessionStatus
  conversationPhase: string | null
  currentProbeDimension: string | null
  completedProbeCount: number
  totalProbeCount: number
  maxFollowUpsPerClaim: number
  inputModality: InputModality
  turns: PublicInterviewTurn[]
}

export interface CreateProjectInterviewPayload {
  mode: 'PROJECT_DEEP_DIVE'
  projectProfileId: number
  durationMinutes: number
  maxFollowUpsPerClaim: number
  inputModality: InputModality
}

export interface SubmitProjectTurnPayload {
  clientTurnId: string
  content: string
  inputModality: InputModality
}

export interface ReportEvidenceRef {
  claimId?: number
  candidateTurnId?: number
  evaluationId?: number
  quote?: string
}

export interface ReportDimension {
  dimension: string
  score: number | null
  status: 'ASSESSED' | 'NOT_ASSESSED'
}

export interface ReportConclusion {
  text: string
  claimId: number
  candidateTurnId: number
  evaluationId: number
}

export interface ClaimReview {
  claimId: number
  strengths: ReportConclusion[]
  risks: ReportConclusion[]
  weaknesses: ReportConclusion[]
  recommendations: ReportConclusion[]
}

export interface TurnReviewItem {
  sequenceNo: number
  dimension: string
  candidateTurnId: number
  candidateAnswer: string
  hitPoints: string[]
  missingPoints: string[]
  riskFlags: string[]
  claimId: number
  evaluationId: number
}

export interface ProjectInterviewReport {
  schemaVersion: number
  sessionId: number
  mode: 'PROJECT_DEEP_DIVE'
  generationStatus: 'COMPLETED'
  totalScore: number | null
  coverageRate: number
  dimensions: ReportDimension[]
  strengths: ReportConclusion[]
  risks: ReportConclusion[]
  weaknesses: ReportConclusion[]
  recommendations: ReportConclusion[]
  claimReviews: ClaimReview[]
  rounds: TurnReviewItem[]
  generatedAt: string
}
