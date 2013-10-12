package util

import scalajdo.DataStore
import javax.jdo.JDOHelper
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory

object UsesDataStore {
  val dataStore: DataStore = new DataStore(() => JDOHelper.getPersistenceManagerFactory("web-scala").asInstanceOf[JDOPersistenceManagerFactory])
}

trait UsesDataStore {
  def dataStore = UsesDataStore.dataStore
}
