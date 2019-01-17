/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution.benchmark

import org.apache.spark.benchmark.Benchmark
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{array, struct}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.types._

/**
 * A benchmark that compares the performance of different ways to evaluate SQL IN expressions.
 *
 * Specifically, this class compares the if-based approach, which might iterate through all items
 * inside the IN value list, to other options with better worst-case time complexities (e.g., sets).
 *
 * To run this benchmark:
 * {{{
 *   1. without sbt: bin/spark-submit --class <this class> <spark sql test jar>
 *   2. build/sbt "sql/test:runMain <this class>"
 *   3. generate result: SPARK_GENERATE_BENCHMARK_FILES=1 build/sbt "sql/test:runMain <this class>"
 *      Results will be written to "benchmarks/InExpressionBenchmark-results.txt".
 * }}}
 */
object InExpressionBenchmark extends SqlBasedBenchmark {

  import spark.implicits._

  private def runByteBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems bytes"
    val values = (Byte.MinValue until Byte.MinValue + numItems).map(v => s"${v}Y")
    val df = spark.range(0, numRows).select($"id".cast(ByteType))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runShortBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems shorts"
    val values = (1 to numItems).map(v => s"${v}S")
    val df = spark.range(0, numRows).select($"id".cast(ShortType))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runIntBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems ints"
    val values = 1 to numItems
    val df = spark.range(0, numRows).select($"id".cast(IntegerType))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runLongBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems longs"
    val values = (1 to numItems).map(v => s"${v}L")
    val df = spark.range(0, numRows).toDF("id")
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runFloatBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems floats"
    val values = (1 to numItems).map(v => s"CAST($v AS float)")
    val df = spark.range(0, numRows).select($"id".cast(FloatType))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runDoubleBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems doubles"
    val values = (1 to numItems).map(v => s"$v.0D")
    val df = spark.range(0, numRows).select($"id".cast(DoubleType))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runSmallDecimalBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems small decimals"
    val values = (1 to numItems).map(v => s"CAST($v AS decimal(12, 1))")
    val df = spark.range(0, numRows).select($"id".cast(DecimalType(12, 1)))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runLargeDecimalBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems large decimals"
    val values = (1 to numItems).map(v => s"9223372036854775812.10539$v")
    val df = spark.range(0, numRows).select($"id".cast(DecimalType(30, 7)))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runStringBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems strings"
    val values = (1 to numItems).map(n => s"'$n'")
    val df = spark.range(0, numRows).select($"id".cast(StringType))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runTimestampBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems timestamps"
    val values = (1 to numItems).map(m => s"CAST('1970-01-01 01:00:00.$m' AS timestamp)")
    val df = spark.range(0, numRows).select($"id".cast(TimestampType))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runDateBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems dates"
    val values = (1 to numItems).map(n => 1970 + n).map(y => s"CAST('$y-01-01' AS date)")
    val df = spark.range(0, numRows).select($"id".cast(TimestampType).cast(DateType))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runArrayBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems arrays"
    val values = (1 to numItems).map(i => s"array($i)")
    val df = spark.range(0, numRows).select(array($"id").as("id"))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runStructBenchmark(numItems: Int, numRows: Long, minNumIters: Int): Unit = {
    val name = s"$numItems structs"
    val values = (1 to numItems).map(i => s"struct($i)")
    val df = spark.range(0, numRows).select(struct($"id".as("col1")).as("id"))
    runBenchmark(name, df, values, numRows, minNumIters)
  }

  private def runBenchmark(
      name: String,
      df: DataFrame,
      values: Seq[Any],
      numRows: Long,
      minNumIters: Int): Unit = {

    val benchmark = new Benchmark(name, numRows, minNumIters, output = output)

    df.createOrReplaceTempView("t")

    def testClosure(): Unit = {
      val df = spark.sql(s"SELECT * FROM t WHERE id IN (${values.mkString(",")})")
      df.queryExecution.toRdd.foreach(_ => Unit)
    }

    benchmark.addCase("In expression") { _ =>
      withSQLConf(SQLConf.OPTIMIZER_INSET_CONVERSION_THRESHOLD.key -> values.size.toString) {
        testClosure()
      }
    }

    benchmark.addCase("InSet expression") { _ =>
      withSQLConf(SQLConf.OPTIMIZER_INSET_CONVERSION_THRESHOLD.key -> "1") {
        testClosure()
      }
    }

    benchmark.run()
  }

  override def runBenchmarkSuite(mainArgs: Array[String]): Unit = {
    val numItemsSeq = Seq(5, 10, 25, 50, 100, 200)
    val largeNumRows = 10000000
    val smallNumRows = 1000000
    val minNumIters = 5

    runBenchmark("In Expression Benchmark") {
      numItemsSeq.foreach { numItems =>
        runByteBenchmark(numItems, largeNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runShortBenchmark(numItems, largeNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runIntBenchmark(numItems, largeNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runLongBenchmark(numItems, largeNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runFloatBenchmark(numItems, largeNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runDoubleBenchmark(numItems, largeNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runSmallDecimalBenchmark(numItems, smallNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runLargeDecimalBenchmark(numItems, smallNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runStringBenchmark(numItems, smallNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runTimestampBenchmark(numItems, largeNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runDateBenchmark(numItems, largeNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runArrayBenchmark(numItems, smallNumRows, minNumIters)
      }
      numItemsSeq.foreach { numItems =>
        runStructBenchmark(numItems, smallNumRows, minNumIters)
      }
    }
  }
}