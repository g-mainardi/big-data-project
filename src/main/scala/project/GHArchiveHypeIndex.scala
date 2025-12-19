package project

import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}
import utils.Commons

object GHArchiveHypeIndex {

  private val path_to_datasets = "gh_data/"
  // Sample file for development (single hour ~10-50 MB)
  private val path_sample = path_to_datasets + "2024-01-01-0.json.gz"
  // Full dataset (all 72 files)
  private val path_full = path_to_datasets + "2024-01-*.json.gz"

  private val path_output_avg_hype_index = "output/hype_reality_index_avg"
  private val path_output_max_hype_index = "output/hype_reality_index_max"


  def main(args: Array[String]): Unit = {
    // Create a SparkConf object; the configuration settings you put here will override those given in the Run/Debug configuration
    val sparkConf = new SparkConf()
      .setAppName("MovieLens job")
      .set("spark.driver.memory", "4g")
//      .set("spark.executor.memory", "4g")
//      .set("spark.executor.cores", "4")
//      .set("spark.executor.instances", "2")
    val spark = SparkSession.builder.config(sparkConf).getOrCreate()

    val sqlContext = spark.sqlContext // needed to save as CSV
    import sqlContext.implicits._

    if(args.length < 2){
      println("The first parameter should indicate the deployment mode (\"local\" or \"remote\")")
      println("The second parameter should indicate the job: "
        + "1 for AvgHypeIndex by Organisation, "
        + "2 for MaxHypeIndex by Organisation, ")
      return
    }

    val deploymentMode = args(0)
    var writeMode = deploymentMode
    if(deploymentMode == "sharedRemote"){
      writeMode = "remote"
    }
    val job = args(1)

    // Initialize input
    val rddEventsSample = spark.sparkContext.textFile(Commons.getDatasetPath(deploymentMode, path_sample))
      .flatMap(line => GHArchiveParser.parseEvent(line))
    val rddEvents = spark.sparkContext.textFile(Commons.getDatasetPath(deploymentMode, path_full))
      .flatMap(line => GHArchiveParser.parseEvent(line))

    import org.apache.spark.storage.StorageLevel
    val rddPAEvents = rddEvents
      .filter(e =>
        GHArchiveParser.isPassive(e.eventType) || GHArchiveParser.isActive(e.eventType)
      )
      .persist(StorageLevel.MEMORY_AND_DISK) // Use DISK spillover for safety

    // Force Cache Materialization (Action)
    val countEvents = rddPAEvents.count()
    println(s"Cached $countEvents valid events.")

    // Configuration for partitioning
    val defaultParallelism = spark.sparkContext.defaultParallelism
    val highPartitions = defaultParallelism * 4 // 16 partitions
    val lowPartitions = defaultParallelism      // 4 partitions

    // Assuming rddPAEvents is RDD[GithubEventData] (the case class we made earlier)
    val hypeIndexPerOrgRepo = rddPAEvents
      .map(event => {
        // Key: (Org, Repo)
        val key = (event.orgLogin, event.repoName)

        // Value: (Passive, Active)
        val value = if (GHArchiveParser.isPassive(event.eventType)) (1, 0) else (0, 1)

        (key, value)
      })
      // Explicitly passing number of partitions to reduceByKey
      .reduceByKey((acc, curr) => (acc._1 + curr._1, acc._2 + curr._2), highPartitions)
      .map { case ((org, _), (passiveCount, activeCount)) =>
        val repoIndex = passiveCount.toDouble / (activeCount + 1)
        (org, repoIndex)
      }
      .persist()

    // Action to force materialization
    println(s"Calculated hype index for ${hypeIndexPerOrgRepo.count()} org-repo pairs")

    if (job=="1"){
      // --- AVERAGE HYPE INDEX PER ORG (OPTIMIZED) ---
      val hypeIndexAvgOpt = hypeIndexPerOrgRepo
        // 1. Prepare for average: (Score) -> (Score, 1)
        .mapValues(score => (score, 1))
        // 2. Reduce: Sum scores and counts
        // Tuple math: v1 is (sumA, countA), v2 is (sumB, countB)
        .reduceByKey((v1, v2) => (v1._1 + v2._1, v1._2 + v2._2), lowPartitions)
        // 3. Calculate Average: (Sum, Count) -> Average
        // Using pattern matching inside mapValues is strictly better than x._1 / x._2
        .mapValues { case (sum, count) => sum / count }
        // 4. Swap for sorting: (Org, Avg) -> (Avg, Org)
        .map(_.swap)
        // 5. Sort Descending
        .sortByKey(ascending = false)

      // --- EXPORT ---
      hypeIndexAvgOpt
        .map(_.swap) // Swap back to (Org, Avg) for the CSV
        .coalesce(1) // Single file output
        .toDF("org_login", "avg_repo_hype_index")
        .write
        .format("csv")
        .mode(SaveMode.Overwrite)
        .option("header", "true")
        .save(Commons.getDatasetPath(writeMode, path_output_avg_hype_index))
    }
    else if (job=="2"){
      val hypeIndexMaxOpt = hypeIndexPerOrgRepo
        // 1. Find Max: Reduce by Key using Math.max
        .reduceByKey((a, b) => math.max(a, b), lowPartitions)
        // 2. Swap for sorting: (Org, Max) -> (Avg, Max)
        .map(_.swap)
        // 3. Sort Descending
        .sortByKey(ascending = false)

      // --- EXPORT ---
      hypeIndexMaxOpt
        .coalesce(1)
        .map(_.swap) // Swap back to (Org, Avg) for the CSV
        .toDF("org_login", "max_repo_hype_index")
        .write
        .format("csv")
        .mode(SaveMode.Overwrite)
        .option("header", "true")
        .save(Commons.getDatasetPath(writeMode, path_output_max_hype_index))
    }
    else {
      println("Wrong job number")
    }

  }

}