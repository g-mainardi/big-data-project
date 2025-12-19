package project

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.util.{Failure, Success, Try}

object GHArchiveParser {

  // Using Sets for O(1) lookups
  private val PASSIVE_EVENTS: Set[String] = Set("Watch", "Fork")

  private val ACTIVE_EVENTS: Set[String] = Set(
    "Push", "PullRequest", "PullRequestReview",
    "PullRequestReviewComment", "CommitComment",
    "Create", "Delete", "Release", "Gollum",
    "Issues", "IssueComment"
  )

  // Lazy initialization of ObjectMapper to avoid serialization issues in Spark
  private lazy val mapper: ObjectMapper = {
    val m = new ObjectMapper()
    m.registerModule(DefaultScalaModule)
    m
  }

  /**
   * Parse a GitHub event JSON line.
   * Returns: Option[GithubEventData]
   */
  def parseEvent(line: String): Option[GithubEventData] = {
    try {
      val root: JsonNode = mapper.readTree(line)

      // Safe extraction handling missing fields
      val eventId = Option(root.get("id")).map(_.asText("")).getOrElse("")

      // Using stripSuffix is cleaner than split for this use case
      val rawType = Option(root.get("type")).map(_.asText("")).getOrElse("")
      val eventType = rawType.stripSuffix("Event")

      // Handle org
      val orgNode = root.get("org")
      val orgLogin = if (orgNode != null && orgNode.has("login")) {
        orgNode.get("login").asText("NO_ORG")
      } else {
        "NO_ORG"
      }

      // Handle repo
      val repoNode = root.get("repo")
      val repoName = if (repoNode != null && repoNode.has("name")) {
        repoNode.get("name").asText("")
      } else {
        ""
      }

      val createdAt = Option(root.get("created_at")).map(_.asText("")).getOrElse("")

      Some(GithubEventData(eventId, eventType, orgLogin, repoName, createdAt))

    } catch {
      case _: Exception => None
    }
  }

  /**
   * Extract date (YYYY-MM-DD) from ISO timestamp
   */
  def extractDate(timestamp: String): Option[String] = {
    // Simple string manipulation is faster than Date parsing for this specific format
    Try {
      timestamp.split("T")(0)
    }.toOption
  }

  def isPassive(eventType: String): Boolean = PASSIVE_EVENTS.contains(eventType)

  def isActive(eventType: String): Boolean = ACTIVE_EVENTS.contains(eventType)
}