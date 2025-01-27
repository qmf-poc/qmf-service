package qmf.poc.service.catalog

case class ObjectDirectory(owner: String,
                           name: String,
                           `type`: String,
                           subType: String,
                           objectLevel: Int,
                           restricted: String,
                           model: String,
                           created: String,
                           modified: String,
                           lastUser: String,
                          )
