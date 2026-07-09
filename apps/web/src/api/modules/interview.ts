import http from '@/api/http'
import type { ApiResponse } from '@/api/http'

export interface CurrentQuestion {
  questionId: number
  cardCode: string
  module: string
  difficulty: string
  questionType: 'MAIN' | 'FOLLOW_UP' | 'EVALUATED'
  questionText: string
  mainQuestion: string
  tags: string
  completedQuestionCount: number
  questionCount: number
}

export interface InterviewSession {
  sessionId: number
  mode: string
  module: string
  difficulty?: string
  questionCount: number
  completedQuestionCount: number
  status: 'IN_PROGRESS' | 'FINISHED'
  currentQuestion?: CurrentQuestion
}

export interface CreateInterviewSessionPayload {
  module: string
  difficulty?: string
  questionCount: number
}

export interface SubmitAnswerPayload {
  answerText: string
}

export interface SubmitAnswerResult {
  answerId: number
  sessionId: number
  questionId: number
  evaluationText: string
  nextAction: 'ASK_FOLLOW_UP' | 'NEXT_QUESTION' | 'FINISH_SESSION'
  followUpQuestion?: string
  currentQuestion?: CurrentQuestion
  completedQuestionCount: number
  questionCount: number
}

export function createInterviewSession(payload: CreateInterviewSessionPayload) {
  return http.post<ApiResponse<InterviewSession>>('/interview-sessions', payload)
}

export function submitInterviewAnswer(sessionId: number, payload: SubmitAnswerPayload) {
  return http.post<ApiResponse<SubmitAnswerResult>>(`/interview-sessions/${sessionId}/answers`, payload)
}

export function nextInterviewQuestion(sessionId: number) {
  return http.post<ApiResponse<InterviewSession>>(`/interview-sessions/${sessionId}/next-question`)
}

export function finishInterviewSession(sessionId: number) {
  return http.post<ApiResponse<InterviewSession>>(`/interview-sessions/${sessionId}/finish`)
}
