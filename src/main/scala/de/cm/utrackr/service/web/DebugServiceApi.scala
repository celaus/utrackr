package de.cm.utrackr.service.web

import de.afs.platform.domain._
import de.afs.platform.logic.{EmotionManager, ModuleSessionManager, ModuleManager, UserManager}
import de.afs.platform.service.web.authentication.UserAuthenticator
import de.afs.platform.service.web.json.DomainObjectProtocol._
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService._
import spray.routing._

import scala.io.Source


trait DebugServiceApi extends UserAuthenticator {

  val userManager: UserManager
  val moduleManager: ModuleManager
  val moduleSessionManager: ModuleSessionManager
  val emotionManager: EmotionManager

  lazy val msm = moduleSessionManager
  val maxEmotions = 13

  var userId = 0

  val debugServiceRoute =
    pathPrefix("debug" / "user") {
      pathPrefix("add") {
        pathEndOrSingleSlash {
          get {
            parameters('insert.as[Boolean] ? false) { insert => if (insert) {
              val user = userManager.save(new User(0, "a@b.com", "password", false, None))
              userId = user.id
              complete {
                user
              }
            }
            else complete("nope")
            }
          }
        }
      } ~
        pathPrefix("clear") {
          pathEndOrSingleSlash {
            get {
              val usr = userManager.findUserById(userId)
              if (usr.isDefined)
                complete {
                  userManager.delete(usr.get)
                }
              else
                complete {
                  "nothing here"
                }
            }
          }
        } ~ pathPrefix("list") {
        pathEndOrSingleSlash {
          get {
            complete {
              "nope"
            }
          }
        }
      }
    } ~
      pathPrefix("debug" / "echo") {
        pathEndOrSingleSlash {
          (get | post) {
            entity(as[String]) { e =>
              complete {
                e
              }
            }
          }
        }
      } ~
      pathPrefix("debug" / "modules") {
        pathPrefix("add") {
          pathEndOrSingleSlash {
            get {
              parameters('name ? "facepuzzle", 'insert.as[Boolean] ? false) { (name, insert) => if (insert) {
                moduleManager.saveModule(Module(0, name))
                complete("ok")
              } else complete("not ok")
              }
            }
          }
        }
      } ~
      pathPrefix("debug" / "emotions") {
        pathPrefix("add") {
          pathEndOrSingleSlash {
            get {
              parameters('insert.as[Boolean] ? false) { insert => if (insert) {
                complete(1 to maxEmotions map (i => {
                  val e = emotionManager.saveEmotion(Emotion(0, s"Emo$i"))
                  (e.id, e.name)
                }))
              } else complete("not ok")
              }
            }
          }
        }
      } ~
      pathPrefix("debug" / "items") {
        pathPrefix("facepuzzle" / "add") {
          pathEndOrSingleSlash {
            get {
              parameters('insert.as[Boolean] ? false) { insert => if (insert) {

                val module = moduleManager.byId(1)

                complete(1 to maxEmotions map (i => {
                  val completed = msm.saveAsset(Asset(0, "video/webm", f"assets/mov/01-KOCEVSKIEmo$i%02d.webm", 0, 0, None, None))
                  val item = msm.saveItem(new Item(0, 2, module.get.id, Some(completed.id), i))

                  msm.saveAsset(Asset(0, "video/webm", f"assets/mov/01-KOCEVSKIEmo$i%02d.L.webm", 0, 0, Some(item.id), Some("lower")))
                  msm.saveAsset(Asset(0, "video/webm", f"assets/mov/01-KOCEVSKIEmo$i%02d.U.webm", 0, 0, Some(item.id), Some("upper")))
                  item.id
                }))
              }
              else complete("NOTHING")
              }
            }
          }
        } ~
          pathPrefix("whospeaks" / "add") {
            pathEndOrSingleSlash {
              get {
                parameters('insert.as[Boolean] ? false) { insert => if (insert) {

                  val module = moduleManager.byId(2)

                  complete(1 to maxEmotions map (i => {
                    val item = msm.saveItem(new Item(0, 2, module.get.id, None, i))

                    msm.saveAsset(Asset(0, "video/webm", f"assets/mov/01-KOCEVSKIEmo$i%02d.webm", 0, 0, Some(item.id), None))

                    msm.saveAsset(Asset(0, "audio/mp3",
                      f"assets/snd/1-Kocevcki_Emo1$i%02d_n.mp3", 1, 0, Some(item.id), None))
                    msm.saveAsset(Asset(0, "audio/mp3",
                      f"assets/snd/1-Kocevcki_Emo1$i%02d_k.mp3", 0, 0, Some(item.id), None))
                    item.id
                  }))
                }
                else complete("NOTHING")
                }
              }
            }
          } ~
          pathPrefix("filmpuzzle" / "add") {
            pathEndOrSingleSlash {
              get {
                parameters('insert.as[Boolean] ? false, 'path.as[String] ? "data/filmpuzzle.csv") { (insert, srcPath) => if (insert) {


                  val src = Source.fromFile(srcPath, "utf-8")
                  val lines = src.getLines().drop(1).map(_.split(";")).map(_.map(_.stripPrefix("\"").stripSuffix("\"").trim
                  ))
                  val module = moduleManager.byId(3)
                  complete(lines map (l => {
                  val noOfParts = l(2).toInt
                    msm.saveAsset(Asset(0, "video/webm", f"assets/mov/filmpuzzle/${l(1) take 2}.webm", 0, 0, None, None))

                    val item = msm.saveItem(new Item(0, noOfParts, module.get.id, None, i))

                    1 to noOfParts foreach (p => {
                      msm.saveAsset(Asset(0, "video/webm", f"assets/mov/filmpuzzle/${l(1)}_$p.webm", 0, p, Some(item.id), None))
                    })
                    item.id
                  }))
                  complete(lines.toSeq)
                }
                else complete("NOTHING")
                }
              }
            }
          }
      }
}