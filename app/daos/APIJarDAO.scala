package daos

import javax.inject._
import java.lang.ProcessBuilder

object APIJarDAO {

  class APIJarDAO @Inject()(
    @NamedDatabase("play") protected val dbConfigProvider: DatabaseConfigProvider
    ) extends HasDatabaseConfigProvider[JdbcProfile] {

    private val scriptName: String = "write-factory.sh"

    def createAPIJar(jarPath: String, username: String): String = {
      // Creates the APIJar object in database given a jar name and username
      // Invokes the write-factory.sh script
      APIJarModel()
    }

    private def invokeWriteFactory(jarPath: String, username: String): Int = {
      val pb: ProcessBuilder = new ProcessBuilder(scriptName, jarPath, username)
      pb.start()
      0
    }

  }

}