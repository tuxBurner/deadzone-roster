package controllers

import javax.inject._

import com.github.tuxBurner.jsAnnotations.JsRoutesComponent
import models.{AbilityDAO, ItemDAO, TroopDAO}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.routing.{JavaScriptReverseRoute, JavaScriptReverseRouter}
import play.i18n.Langs

import scala.collection.JavaConversions._


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, jsRoutesComponent: JsRoutesComponent, langs: Langs) extends AbstractController(cc) with I18nSupport {


  /**
    * Display the main view
    *
    * @return
    */
  def rosterMain = Action {
    implicit request =>
      Ok(views.html.roster())
  }

  /**
    * Change the language of the current user
    *
    * @param language
    * @return
    */
  def changeLanguage(language: String) = Action {
    request =>

      val redirectTo: String = request.headers.get(REFERER).getOrElse(routes.HomeController.rosterMain().url)

      val lang = langs.availables()
        .find(_.code == language)
        .getOrElse(langs.availables().get(0))

      Redirect(redirectTo).withLang(lang)
  }

  /**
    * Displays all abilities in a table overview
    *
    * @return
    */
  def displayAllAbilities() = Action {
    implicit request =>
      val abilities = AbilityDAO.findAll()
      Ok(views.html.allAbilities(abilities))
  }

  /**
    * Displays all available army specials
    *
    * @return
    */
  def displayAllArmySpecials() = Action {
    implicit request =>
      val troopsWithArmySpecials = TroopDAO.findAllWithArmySpecials()
      Ok(views.html.allArmySpecials(troopsWithArmySpecials))
  }

  /**
    * Display all items
    *
    * @return
    */
  def displayAllItems() = Action {
    implicit request =>
      val items = ItemDAO.findAllItems()
      Ok(views.html.allItems(items))
  }


  /**
    * Register the routes to certain stuff to the javascript routing so we can
    * reach it better from there
    *
    * @return
    */
  def jsRoutes = Action {
    request =>
      val routes: Array[JavaScriptReverseRoute] = jsRoutesComponent.getJsRoutes.toSet.toArray
      val routeScript = JavaScriptReverseRouter.apply("jsRoutes", Some("jQuery.ajax"), request.host, routes: _*)

      Ok(routeScript.body).as("text/javascript")
  }


}
