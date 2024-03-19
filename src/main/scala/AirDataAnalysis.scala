import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object AirDataAnalysis {
  def main(args:Array[String]): Unit = {
    // Create a SparkSession
    val spark = SparkSession.builder()
      .appName("AirDataAnalysis")
      .master("local[*]")
      .getOrCreate()

    val schema = new StructType(

      Array(
        StructField("ActualElapsedTime", DoubleType, nullable = true),
        StructField("AirTime", DoubleType, nullable = true),
        StructField("ArrDelay", DoubleType, nullable = true),
        StructField("ArrTime", DoubleType, nullable = true),
        StructField("CRSArrTime", DoubleType, nullable = true),
        StructField("CRSDepTime", DoubleType, nullable = true),
        StructField("CRSElapsedTime", DoubleType, nullable = true),
        StructField("CancellationCode", StringType, nullable = true),
        StructField("Cancelled", IntegerType, nullable = true),
        StructField("CarrierDelay", DoubleType, nullable = true),
        StructField("DayOfWeek", DoubleType, nullable = true),
        StructField("DayofMonth", DoubleType, nullable = true),
        StructField("DepDelay", DoubleType, nullable = true),
        StructField("DepTime", DoubleType, nullable = true),
        StructField("Dest", DoubleType, nullable = true),
        StructField("Distance", DoubleType, nullable = true),
        StructField("Diverted", DoubleType, nullable = true),
        StructField("FlightNum", DoubleType, nullable = true),
        StructField("LateAircraftDelay", DoubleType, nullable = true),
        StructField("Month", DoubleType, nullable = true),
        StructField("NASDelay", DoubleType, nullable = true),
        StructField("Origin", StringType, nullable = true),
        StructField("SecurityDelay", DoubleType, nullable = true),
        StructField("TailNum", DoubleType, nullable = true),
        StructField("TaxiIn", DoubleType, nullable = true),
        StructField("TaxiOut", DoubleType, nullable = true),
        StructField("UniqueCarrier", StringType, nullable = true),
        StructField("WeatherDelay", DoubleType, nullable = true),
        StructField("Year", IntegerType, nullable = true),
      )
    )
    // Read the CSV file into a DataFrame

    val df = spark.read
      .format("csv")
      .schema(schema)
      .option("header", "true")
      // Treats the first line as a header
      .load("C:\\Users\\HP\\Desktop\\airline-dataset.csv")

    /* 1. What is the average delay time for each airline carrier?
          Calculate average delay by UniqueCarrier */

   val averageDelayByUniqueCarrier = df.
     withColumn("TotalDelay", col("ArrDelay") + col("DepDelay") + col("CarrierDelay") + col("WeatherDelay") + col("NASDelay") + col("SecurityDelay") + col("LateAircraftDelay"))
     .where(col("TotalDelay") > 0 && col("UniqueCarrier").isNotNull)
     .groupBy("UniqueCarrier") // Grouping the DataFrame by UniqueCarrier column
      .agg(avg("TotalDelay").alias("AvgDelayBasedOnCarrier")
      )
    /* 2. Is there any correlation between distance and delay time?
          Calculate the correlation between "Distance" and "ArrDelay" columns */

    val correlationValueDistanceandArrDelay = df.stat.corr("distance", "ArrDelay")

    /* 3. How does the number of flights and cancellations vary over the years?
          Calculate number of flights and cancellations by year */

    val flightsByYearCancellations = df.groupBy((col("Year")).alias("Year"))
      .agg(count("*").alias("NumFlights"), sum(when(col("Cancelled") === 1, 1).otherwise(0)).alias("NumCancellations"))

    //4. Which airport has the highest number of delays/cancellations?

    val airportDelaysCancellations = df.groupBy("Origin")
      .agg(sum("ArrDelay").alias("TotalDelay"), sum("Cancelled").alias("TotalCancellations"))
      .orderBy(desc("TotalDelay"))
      .limit(1)

    /* 5. Is there a significant difference in delay time between weekdays and weekends?
          Calculate the average delay time for weekdays and weekends */

    val avgWeekdayDelay = df.filter(col("DayOfWeek").between(1, 5))
      .agg(avg("ArrDelay").alias("AvgWeekdayArrDelay"), avg("DepDelay").alias("AvgWeekdayDepDelay"))

    val avgWeekendDelay = df.filter(col("DayOfWeek").between(6, 7))
      .agg(avg("ArrDelay").alias("AvgWeekendArrDelay"), avg("DepDelay").alias("AvgWeekendDepDelay"))

    // Subtract the average weekday delay from the average weekend delay

    val delayDifference = avgWeekendDelay.crossJoin(avgWeekdayDelay)
      .select(
        col("AvgWeekendArrDelay") - col("AvgWeekdayArrDelay").alias("ArrDelayDifference"),
        col("AvgWeekendDepDelay") - col("AvgWeekdayDepDelay").alias("DepDelayDifference")
      )


    /* 6. Which month has the highest number of flight cancellations?
          Filter cancelled flights */

    val cancelledFlights = df.filter("Cancelled = 1")

    // Group by month and count the number of cancellations

    val cancellationsByMonth = cancelledFlights.groupBy("Month")
      .agg(count("*").alias("CancellationCount"))
      .orderBy(desc("CancellationCount"))

    // Find the month with the highest number of flight cancellations

    val highestCancellationMonth = cancellationsByMonth.select("Month", "CancellationCount")
      .first()

    /* 7. Can you identify any patterns in the reasons for flight cancellations?
          Filter the DataFrame to include only cancelled flights (where Cancelled column equals 1)
          Group the filtered DataFrame by the "CancellationCode" column
          Count the occurrences of each unique cancellation code
    */

    val cancellationPatterns = df.filter("Cancelled = 1")
      .groupBy("CancellationCode")
      .count()

    /* 8. Is there any relationship between the length of the flight and the amount of time spent taxiing?
          Group the DataFrame by the "Distance" column
          Calculate the average taxi-in time for each group (distance)
          Calculate the average taxi-out time for each group (distance)
          Order the DataFrame by the "Distance" column in ascending order
    */

    val taxiingTimeByDistance = df.groupBy("Distance")
      .agg(avg("TaxiIn").alias("AvgTaxiIn"), avg("TaxiOut").alias("AvgTaxiOut"))
      .orderBy("Distance")

    /* 9. How does the average delay time vary between different airports?
          Group the DataFrame by the "Origin" column, which represents the airports of departure
          Calculate the average departure delay for each airport
          Calculate the variance of departure delay for each airport
    */

    val varianceDelayByAirport = df.groupBy("Origin")
      .agg(
        avg("DepDelay").alias("AvgDepDelay"),
        variance("DepDelay").alias("VarianceDepDelay")
      )


    /* 10. Do older planes suffer more delays?
           Calculate the date using Year, Month, and DayofMonth columns */
    val cData = df.withColumn("DayofMonth", col("DayofMonth").cast("int"))
      .withColumn("Month", col("Month").cast("int"))
      .withColumn("Year", col("Year").cast("int"))

    val date = df.withColumn("Date", to_date(concat(col("Year"), lit("-"), col("Month"), lit("-"), col("DayofMonth"))))

    val firstFlyDates = date.groupBy("FlightNum")
      .agg(min("Date").alias("FirstFlyDate"))
    val joinedData = date.join(firstFlyDates, Seq("FlightNum"))

    val dateDiff = joinedData.withColumn("AgeInMonthSinceFirstFly", months_between(col("FirstFlyDate"), col("Date")))

    val groupedData1 = dateDiff.groupBy("AgeInMonthSinceFirstFly").agg(avg("LateAircraftDelay").alias("AgeBasedAvgLateAircraftDelay"))
    val correlationValueAgeandDelay = groupedData1.stat.corr("AgeInMonthSinceFirstFly", "AgeBasedAvgLateAircraftDelay")


    /* 11. What is the distribution of delay times for different airlines?
           Calculate summary statistics for delay columns by unique carrier */

    val delayDFNotNull = df.filter(col("ArrDelay").isNotNull || col("DepDelay").isNotNull)
    val delayStatisticsSummary = delayDFNotNull.groupBy("UniqueCarrier")
      .agg(

        avg("ArrDelay").as("AvgArrDelay"),

        stddev("ArrDelay").as("StdDevArrDelay"),

        min("ArrDelay").as("MinArrDelay"),

        max("ArrDelay").as("MaxArrDelay"),

      )

    /* 12. How well does weather predict plane delays?

    Selecting specific columns "WeatherDelay" and "ArrDelay" from the DataFrame df */

    val selectedData = df.select("WeatherDelay", "ArrDelay")

    // Filter out rows with missing values

    val filteredData = selectedData.na.drop()

    // Calculate the correlation between "WeatherDelay" and "ArrDelay" columns in the filteredData DataFrame

    val correlationBetweenWeatherDelayandArrDelay = filteredData.stat.corr("WeatherDelay", "ArrDelay")


    // Show the results
    println("The average delay time for each airline carrier")
    averageDelayByUniqueCarrier.collect().foreach(row => {
      val airlineCarrier =row.getString(0)
      val averageDelay = row.getDouble(1)
      println(s"airlineCarrier: $airlineCarrier,  Avg_Delay: $averageDelay")
    })

    if (correlationValueDistanceandArrDelay == 0) {
      println("It indicated that there is no correlation between Distance and ArrDelay")
    }
    else if (correlationValueDistanceandArrDelay > 0 & correlationValueDistanceandArrDelay <= 1) {
      println("It shows that there is a strong correlation exists between Distance and ArrDelay")
    }
    else {
      println("It shows there is very week correlation exists between Distance and ArrDelay")
    }


    flightsByYearCancellations.show()
    airportDelaysCancellations.show()
    delayDifference.show()
    println(s"The month with the highest number of flight cancellations is ${highestCancellationMonth(0)} with ${highestCancellationMonth(1)} cancellations.")
    cancellationPatterns.show()
    taxiingTimeByDistance.show()
    varianceDelayByAirport.show()

    if (correlationValueAgeandDelay == 0) {
      println("It indicated that there is no correlation between Age and Delay")
    }
    else if (correlationValueAgeandDelay > 0 & correlationValueAgeandDelay <= 1) {
      println("It shows that there is a strong correlation exists between Age and Delay. Older planes suffer more delays.")
    }
    else {
      println("It shows there is very week correlation exists between Age and Delay")
    }

    delayStatisticsSummary.show()

    if (correlationBetweenWeatherDelayandArrDelay == 0) {
      println("It indicated that there is no correlation between Weather and ArrDelay")
    }
    else if (correlationBetweenWeatherDelayandArrDelay > 0 & correlationBetweenWeatherDelayandArrDelay <= 1) {
      println("It shows that there is a strong correlation exists between Weather and ArrDelay")
    }
    else {
      println("It shows there is very week correlation exists between Weather and ArrDelay")
    }
    // Stop the SparkSession

    spark.stop()
  }
}
