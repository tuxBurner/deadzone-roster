package models

import javax.persistence._
import javax.validation.constraints.NotNull

import com.avaje.ebean.Model
import deadzone.models.CSVModels.CSVSoldierDto
import play.api.Logger

import scala.collection.JavaConversions._

/**
  * @author Sebastian Hardt (s.hardt@micromata.de)
  *         Date: 07.06.17
  *         Time: 21:29
  */
object TroopDAO {

  private val FINDER = new Model.Finder[Long, TroopDO](classOf[TroopDO])


  def deleteAll(): Unit = {
    Logger.info("Deleting all: " + classOf[TroopDO].getName + " from database")
    FINDER.all().toList.foreach(troopDo => {
      troopDo.defaultWeapons.clear()
      troopDo.allowedWeaponTypes.clear()
      troopDo.defaultItems.clear()
      troopDo.update()
      troopDo.delete()
    })
  }

  def findAllForFactionByName(factionName: String): List[TroopDO] = {
    FINDER.where().eq("faction.name", factionName).findList.toList
  }

  def findByFactionAndName(factionName: String, name:String): TroopDO = {
    FINDER.where().eq("faction.name", factionName).and().eq("name",name).findUnique
  }

  /**
    * Finds all troops with an army special in the database
    * @return
    */
  def findAllWithArmySpecials() : List[TroopDO] = {
    FINDER.where().ne("armySpecial","").order().asc("faction.name").orderBy().asc("armySpecial").findList().toList
  }

  /**
    * Adds a troop to the databse from the csv information's
    * @param soldierDto
    * @param factionDo
    * @return
    */
  def addFromCSVSoldierDto(soldierDto: CSVSoldierDto, factionDo: FactionDO): TroopDO = {
    Logger.info("Creating troop: " + soldierDto.name + " for faction: " + factionDo.name)

    val troopDO = new TroopDO()
    troopDO.faction = factionDo
    troopDO.name = soldierDto.name
    troopDO.points = soldierDto.points
    troopDO.modelType = soldierDto.soldierType.toString
    troopDO.speed = soldierDto.speed._1
    troopDO.sprint = soldierDto.speed._2
    troopDO.shoot = soldierDto.shoot
    troopDO.fight = soldierDto.fight
    troopDO.survive = soldierDto.survive
    troopDO.size = soldierDto.size
    troopDO.armour = soldierDto.armour
    troopDO.victoryPoints = soldierDto.victoryPoints
    troopDO.hardPoints = soldierDto.hardPoints
    troopDO.recon = soldierDto.recon
    troopDO.armySpecial = soldierDto.armySpecial
    troopDO.imageUrl = soldierDto.imageUrl

    // find the weapons
    soldierDto.defaultWeaponNames.foreach(weaponName => {
      val defaultWeapon = WeaponDAO.findByNameAndFactionAndAllowedTypes(weaponName, factionDo,soldierDto.weaponTypes.toList)
      if (defaultWeapon == null) {
        Logger.error("Could not add default weapon " + weaponName + " to troop: " + troopDO.name + " faction: " + factionDo.name + " was not found in the db")
      } else {
        troopDO.defaultWeapons.add(defaultWeapon)
      }
    })

    soldierDto.weaponTypes.foreach(weaponTypeName => {
      val weaponTypeDo = WeaponTypeDAO.findOrCreateTypeByName(weaponTypeName)
      troopDO.allowedWeaponTypes.add(weaponTypeDo);
    })

    soldierDto.defaultItems.foreach(itemName => {
      val itemDo = ItemDAO.findByNameAndFaction(itemName,factionDo)
      if(itemDo == null)  {
        Logger.error("Troop: "+soldierDto.name+" in faction: "+soldierDto.faction+" cannot find item: "+itemName+" in DB.")
      } else {
        troopDO.defaultItems.add(itemDo)
      }
    })

    troopDO.save()

    soldierDto.abilities.foreach(DefaultTroopAbilityDAO.addAbilityForTroop(troopDO, _))

    troopDO
  }

}

@Entity
@Table(name = "troop") class TroopDO extends Model {

  @Id val id: Long = 0L

  @NotNull var name: String = ""

  @NotNull var points: Int = 0

  @NotNull var modelType: String = ""

  @NotNull var speed: Int = 0

  @NotNull var sprint: Int = 0

  @NotNull var shoot: Int = 0

  @NotNull var fight: Int = 0

  @NotNull var survive: Int = 0

  @NotNull var size: Int = 0

  @NotNull var armour: Int = 0

  @NotNull var victoryPoints: Int = 0

  @NotNull var hardPoints: Int = 0

  @NotNull var recon: Int = 0

  @NotNull var armySpecial: String = ""

  var imageUrl:String = ""

  @ManyToOne var faction: FactionDO = null

  @ManyToMany
  @JoinTable(name = "def_troop_weapon") var defaultWeapons: java.util.List[WeaponDO] = null

  @OneToMany
  var defaultTroopAbilities: java.util.List[DefaultTroopAbilityDO] = null

  @ManyToMany
  var allowedWeaponTypes: java.util.List[WeaponTypeDO] = null

  @ManyToMany
  var defaultItems: java.util.List[ItemDO] = null
}
