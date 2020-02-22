package clients

import com.redis._
import play.api.libs.functional.syntax._
import play.api.libs.json._


/*
 * Class for managing a Redis pool
 */
object RedisSetClient {
  /*
   * Defines an object to standardize responses from Redis
   *
   * @param body Response body, None when error
   * @param error Error message, None when success
   * @param key Redis Key for which set is operated on
   * @param status 0 for success, 1 for all errors (for now)
   */
  case class RedisResponse(
    body: Option[String], error: Option[String], key: String, status: Int)

  class RedisSetClient(val key: String) (implicit val host: String, implicit val port: Int) {

    val client: RedisClient = new RedisClient(host, port)

    /*
     * Adds an item to the given set in Redis
     */
    def add(item: String): RedisResponse = {
      val numItemsAdded: Option[Long] = client.sadd(this.key, item)
      numItemsAdded match {
        case Some(n) => n match {
          case 1 => RedisResponse(Some(""), None, this.key, 0)
          case 0 => RedisResponse(
            None, Some("Unexpected 0 items inserted"), this.key, 1)
          case _ => RedisResponse(
            Some("Potential double insert: "+n), None, this.key, 0)
        }
        case None => RedisResponse(None, Some("Redis SADD Error"), this.key, 1)
      }
    }

    /*
     * Removes an item from the given set in Redis
     */
    def remove(item: String): RedisResponse = {
      val numItemsRemoved: Option[Long] = client.srem(this.key, item)
      numItemsRemoved match {
        case Some(n) => n match {
          case 1 => RedisResponse(Some(""), None, this.key, 0)
          case 0 => RedisResponse(
            None, Some("Unexpected 0 items removed"), this.key, 1)
          case _ => RedisResponse(
            Some("Potential overremoval of this many items: "+n), None, this.key, 0)
        }
        case None => RedisResponse(None, Some("Redis SREM Error"), this.key, 1)
      }
    }

    // ==============================================================
    // Begin ReadAll functions. Can return an iterator, a set of JSON
    // Strings, or a set of JSON objects
    // Reads all items from the given set in Redis
    // ==============================================================

    def readAllIterator(): Iterator[Option[String]] = {
      println("\t\tRedis Key is: " + this.key)
      client.smembers(
      this.key).get.iterator
    }

    def readAllSet(): Set[String] = this.readAllIterator().toSet.flatten

    def readAllJson(): Set[JsValue] = {
      val allSet = this.readAllSet()
      println("\t\tAll In Set: " + allSet)
      allSet.map(Json.parse)
    }
  }
}
