export interface AuthUser {
  id: number
  username: string
}

export interface AuthPayload {
  accessToken: string
  accessTokenExpiresAt: string
  user: AuthUser
}
