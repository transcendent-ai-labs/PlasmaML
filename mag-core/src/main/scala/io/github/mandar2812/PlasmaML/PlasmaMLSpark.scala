package io.github.mandar2812.PlasmaML

import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author mandar2812 date 14/5/16.
  * */
object PlasmaMLSpark {

  /**
    * The number of spark executors that can be spawned, which can be
    * the number of cores available on the machine at maximum.
    * */
  var sparkCores = 4
  val sparkHost = "local["+sparkCores+"]"

  val sc = new SparkContext(
    new SparkConf().setMaster(sparkHost)
      .setAppName("PlasmaML on Apache Spark")
      .set("spark.executor.memory", "1g"))


}
