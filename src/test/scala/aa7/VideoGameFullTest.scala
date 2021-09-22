package aa7

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
import scala.util.Random

class VideoGameFullTest extends Simulation {

  val httpConf = http
    .baseUrl("http://localhost:8080/app/")
    .header("Accept", "application/json")

  /*** Variables ***/
  // runtime variables
  def userCount: Int = getProperty("USERS", "5").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int = getProperty("DURATION", "60").toInt

  // other variables
  val idNumbers = (1 to 10).iterator
  val rnd = new Random()
  val now = LocalDate.now()
  val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /*** Helper Methods ***/
  private def getProperty(propertyName: String, defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  def randomString(length: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  def getRandomDate(startDate: LocalDate, random: Random): String = {
    startDate.minusDays(random.nextInt(30)).format(pattern)
  }

  /*** Custom Feeder ***/
  val customFeeder = Iterator.continually(Map(
    "gameId" -> idNumbers.next(),
    "name" -> ("GameName-" + randomString(5)),
    "releaseDate" -> getRandomDate(now, rnd),
    "reviewScore" -> rnd.nextInt(100),
    "category" -> ("Category-" + randomString(5)),
    "rating" -> ("Rating-" + randomString(5))
  ))

  /*** Before ***/
  before {
    println(s"Running test with ${userCount} users")
    println(s"Ramping users over ${rampDuration} seconds")
    println(s"Total Test duration: ${testDuration} seconds")
  }

  /*** HTTP Calls ***/
  def getAllVideoGames(): ChainBuilder = {
    exec(
      http("Get All Video Games")
        .get("videogames")
        .check(status.is(200)))
  }

  def postNewGame(): ChainBuilder = {
    feed(customFeeder).
      exec(http("Post New Game")
        .post("videogames")
        .body(ElFileBody("bodies/NewGameTemplate.json")).asJson //template file goes in gatling/resources/bodies
        .check(status.is(200)))
      .pause(1)
  }

  def getLastPostedGame(): ChainBuilder = {
    exec(http("Get recent game")
      .get("videogames/${gameId}")
      .check(jsonPath("$.name").is("${name}"))
      .check(status.is(200)))
  }

  def deleteLastPostedGame() = {
    exec(http("Delete Last Posted Game")
      .delete("videogames/${gameId}")
      .check(status.is(200)))
      //.exec(
      //  http("Confirm game deleted")
      //    .get("videogames/${gameId}")
      //    .check(status.is(500))
      //)
    }

  /*** Scenario Design ***/

  val scn = scenario("Create and delete games flow")
    .forever() {
      exec(getAllVideoGames())
        .pause(2)
        .exec(postNewGame())
        .pause(2)
        .exec(getLastPostedGame())
        .pause(2)
        .exec(deleteLastPostedGame())
    }

  /*** Setup Load Simulation ***/
  setUp(
    scn.inject(
      nothingFor(5.seconds),
      rampUsers(userCount) during (rampDuration.seconds))
  )
    .protocols(httpConf)
    .maxDuration(testDuration.seconds)

  /*** After ***/
  after {
    println("Stress test completed")
  }

  // SCENARIO Design
  // 1. Get all game
  // 2. Create a new game
  // 3. Get details of that single game
  // 4. Delete the game


  // SET UP LOAD SIMULATION

  // Create a scenario that has run time params for
  // users
  // rampup time
  // test duration

  // Custom Feeder
  // To generate the date for the new game

  // Helper methods
  // For the custom feeder or the run time params

  // VARIABLES
  // For the helper methods


  // BEFORE AND AFTER
  //To print out message at start and at the end

}
