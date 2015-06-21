package de.cm.utrackr.dao



trait ActivityDAO {

}


trait ActivityDAOComponent {

  def dao: ActivityDAOInterface

  trait ActivityDAOInterface extends ActivityDAO

}