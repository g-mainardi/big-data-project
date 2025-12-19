package project

case class GithubEventData(
                            eventId: String,
                            eventType: String,
                            orgLogin: String,
                            repoName: String,
                            createdAt: String
                          )