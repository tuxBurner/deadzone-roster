package services.logic

import java.util.UUID

import models._
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable.ListBuffer

/**
  * Created by tuxburner on 12.06.17.
  */
object ArmyLogic {

  /**
    * Changes the name of the given army
    *
    * @param armyName the name of the army to set
    * @param army the army itself
    * @return
    */
  def changeNameOfArmy(armyName: String, army: ArmyDto): ArmyDto = {
    army.copy(name = armyName.trim)
  }

  /**
    * Adds a new troop to the army
    *
    * @param factionName the name of the faction
    * @param troopName the name of the troop to add
    * @param army the army where to add the troop to
    * @return
    */
  def addTroopToArmy(factionName: String, troopName: String, army: ArmyDto): ArmyDto = {
    val troopDo = TroopDAO.findByFactionAndName(factionName, troopName)

    val newTroop = troopDoToArmyTroopDto(troopDo.get)

    val newTroops: List[ArmyTroopDto] = army.troops :+ newTroop
    val armyPoints = newTroops.map(_.points).sum

    val factions = getFactionsFromArmy(newTroops)

    army.copy(troops = newTroops, faction = factions, points = armyPoints)
  }

  /**
    * Collects all factions from the troops as a a comma separated string
    *
    * @param troops get all factions from the given troops
    * @return
    */
  def getFactionsFromArmy(troops: List[ArmyTroopDto]): String = {
    troops.map(_.faction).distinct.mkString(",")
  }

  /**
    * Removes the given troop from the army
    *
    * @param uuid the uuid of the troop to remove from the army
    * @param army the army containing the troop
    * @return
    */
  def removeTroopFromArmy(uuid: String, army: ArmyDto): ArmyDto = {
    val newTroops = army.troops.filter(_.uuid != uuid)
    val armyPoints = newTroops.map(_.points).sum
    val faction = if (newTroops.isEmpty) "" else getFactionsFromArmy(newTroops)
    army.copy(troops = newTroops, faction = faction, points = armyPoints)
  }

  /**
    * Clones the given troop and adds it to the army
    *
    * @param uuid the uuid of the troop to clone
    * @param army the army containing the troop to clone and where to add the cloned troop
    * @return
    */
  def cloneTroop(uuid: String, army: ArmyDto): ArmyDto = {
    val troopToClone = getTroopFromArmy(uuid, army)

    val newTroops = army.troops :+ troopToClone.copy(uuid = UUID.randomUUID().toString)

    val armyPoints = newTroops.map(_.points).sum

    army.copy(troops = newTroops, points = armyPoints)
  }

  /**
    * Updates the troop with the given items and weapons
    *
    * @param uuid
    * @param army
    * @param weapons
    * @param items
    * @return
    */
  def updateTroop(uuid: String, army: ArmyDto, weapons: List[String], items: List[String]): ArmyDto = {
    val currentTroop = getTroopFromArmy(uuid, army)
    val newWeapons = weapons.map(weaponName => {
      val weaponDo = WeaponDAO.findByNameAndFactionNameAndAllowedTypes(weaponName, currentTroop.faction, currentTroop.allowedWeaponTypes)
      weaponDoToWeaponDto(weaponDo.get)
    })

    val newItems = items.map(itemName => {
      val itemDo = ItemDAO.findByNameAndFactionName(itemName, currentTroop.faction)
      itemDoToItemDto(itemDo.get)
    })


    val points = currentTroop.basePoints + newWeapons.map(_.points).sum + newItems.map(_.points).sum
    val victoryPoints = currentTroop.baseVictoryPoints + newWeapons.map(_.victoryPoints).sum

    val newTroops = army.troops.map(troop => {
      if (troop.uuid != uuid) troop else {
        // add all items which are in the old troop and no upgrade item
        val itemsToSet = newItems ++ troop.items.filter(_.noUpdate == true)
        troop.copy(points = points, victoryPoints = victoryPoints, weapons = newWeapons, items = itemsToSet)
      }
    })

    val armyPoints = newTroops.map(_.points).sum

    army.copy(points = armyPoints, troops = newTroops)
  }

  /**
    * Converts the troop database object to the dto
    *
    * @param troopDo
    * @return
    */
  def troopDoToArmyTroopDto(troopDo: TroopDO): ArmyTroopDto = {

    val troopAbilities = troopDo.defaultTroopAbilities.map(abilityDo => ArmyAbilityDto(abilityDo.ability.name, abilityDo.defaultValue))
    val weapons = troopDo.defaultWeapons.map(weaponDoToWeaponDto(_))

    val points = troopDo.soldierDto.points + troopDo.defaultWeapons.map(_.points).sum + troopDo.defaultItems.map(_.points).sum
    val victoryPoints = troopDo.soldierDto.victoryPoints + troopDo.defaultWeapons.map(_.victoryPoints).sum

    val weaponTypes = troopDo.allowedWeaponTypes.map(_.name)

    val items = troopDo.defaultItems.map(itemDoToItemDto(_))


    ArmyTroopDto(
      uuid = UUID.randomUUID().toString,
      faction = troopDo.faction.name,
      name = troopDo.soldierDto.name,
      modelType = troopDo.soldierDto.soldierType.toString,
      basePoints = troopDo.soldierDto.points,
      points = points,
      baseVictoryPoints = troopDo.soldierDto.victoryPoints,
      victoryPoints = victoryPoints,
      speed = troopDo.soldierDto.speed._1,
      sprint = troopDo.soldierDto.speed._2,
      armour = troopDo.soldierDto.armour,
      size = troopDo.soldierDto.size,
      shoot = troopDo.soldierDto.shoot,
      fight = troopDo.soldierDto.fight,
      survive = troopDo.soldierDto.survive,
      abilities = troopAbilities,
      weapons = weapons,
      items = items,
      allowedWeaponTypes = weaponTypes,
      recon = troopDo.soldierDto.recon,
      armySpecial = troopDo.soldierDto.armySpecial,
      defaultWeapons = weapons)

  }

  /**
    * Gets the weapons and items which are allowed for the given uui troop
    * Also returns the currently selected items and weapons
    *
    * @param uuid
    * @param army
    * @return
    */
  def getWeaponsAndItemsForTroop(uuid: String, army: ArmyDto): ArmyTroopWeaponsItemsDto = {

    val troopDto = getTroopFromArmy(uuid, army)
    val weapons = getWeaponsForTroop(troopDto)
    val items = getItemsForTroop(troopDto)

    ArmyTroopWeaponsItemsDto(weapons, items, troopDto)
  }


  /**
    * Gets all available weapon options for the given troop
    *
    * @param troopDto the troop which the weapons are for
    * @return
    */
  def getWeaponsForTroop(troopDto: ArmyTroopDto): Map[String, List[ArmyWeaponDto]] = {
    val weapons = WeaponDAO.findByFactionAndTypes(troopDto.faction, troopDto.allowedWeaponTypes)

    val rangedWeaopns = weapons.filter(weaponDo => weaponDo.shootRange != 0 && weaponDo.free == false && StringUtils.isBlank(weaponDo.linkedName)).map(weaponDoToWeaponDto(_))
    val fightWeapons = weapons.filter(weaponDo => weaponDo.shootRange == 0 && weaponDo.free == false && StringUtils.isBlank(weaponDo.linkedName)).map(weaponDoToWeaponDto(_))
    val freeWeapons = weapons.filter(_.free == true).map(weaponDoToWeaponDto(_))
    val linkedWeapons = weapons.filter(weaponDo => weaponDo.free == false && StringUtils.isNoneBlank(weaponDo.linkedName)).map(weaponDoToWeaponDto(_))


    Map("ranged" -> rangedWeaopns, "fight" -> fightWeapons, "free" -> freeWeapons, "linked" -> linkedWeapons)
  }

  /**
    * Gets the troop from the given army by its uuid
    *
    * @param uuid
    * @param army
    */
  private def getTroopFromArmy(uuid: String, army: ArmyDto): ArmyTroopDto = {
    army.troops.find(_.uuid == uuid).get
  }

  /**
    * Gets the items for the given troop
    *
    * @param troop
    * @return
    */
  def getItemsForTroop(troop: ArmyTroopDto): List[ArmyItemDto] = {
    if (troop.abilities.find(ability => ability.name == "Beast" || ability.name == "Vehicle").isDefined) {
      return List()
    }
    ItemDAO.findAllItemsForFaction(troop.faction).map(itemDoToItemDto(_))
  }

  /**
    * Transforms an item from the backend to an item for the frontend.
    *
    * @param itemDo
    * @return
    */
  def itemDoToItemDto(itemDo: ItemDO): ArmyItemDto = {
    ArmyItemDto(itemDo.name, itemDo.points, itemDo.rarity, itemDo.noUpdate)
  }

  /**
    * Transforms a weapon database object to a weapon dto
    *
    * @param weaponDo
    * @return
    */
  def weaponDoToWeaponDto(weaponDo: WeaponDO): ArmyWeaponDto = {
    val abilities = weaponDo.defaultWeaponAbilities.map(abilityDo => ArmyAbilityDto(abilityDo.abilityDO.name, abilityDo.defaultValue))
    ArmyWeaponDto(weaponDo.name, weaponDo.points, weaponDo.shootRange, weaponDo.armorPircing, weaponDo.victoryPoints, abilities, weaponDo.free, weaponDo.linkedName)
  }

  /**
    * Extracts informations about the army from it for pdf informations
    *
    * @param army
    * @return
    */
  def extractPdfArmyInfos(army: ArmyDto): ArmyPdfInfos = {
    val abilitiesBuffer = ListBuffer[String]()
    val itemsBuffer = ListBuffer[String]()
    army.troops.foreach(troop => {
      troop.abilities.foreach(ability => {
        abilitiesBuffer += ability.name
      })

      troop.items.foreach(item => {
        itemsBuffer += item.name
      })

      troop.weapons.foreach(weapon => {
        weapon.abilities.foreach(ability => abilitiesBuffer += ability.name)
      })
    })
    val abilities = abilitiesBuffer.toList.distinct.sortWith(_ < _)

    val items = itemsBuffer.toList.distinct.sortWith(_ < _)

    val reconVals = army.troops.filter(_.recon != 0).map(troop => (troop.name, troop.recon, troop.armySpecial)).distinct.sortWith(_._3 < _._3)

    ArmyPdfInfos(abilities, items, reconVals)
  }

}

case class ArmyDto(name: String,
                   faction: String = "",
                   points: Int = 0,
                   troops: List[ArmyTroopDto] = List())

case class ArmyAbilityDto(name: String, defaultVal: Int)

case class ArmyTroopDto(uuid: String,
                        faction: String,
                        name: String,
                        modelType: String,
                        basePoints: Int,
                        points: Int,
                        baseVictoryPoints: Int,
                        victoryPoints: Int,
                        speed: Int,
                        sprint: Int,
                        armour: Int,
                        size: Int,
                        shoot: Int,
                        fight: Int,
                        survive: Int,
                        abilities: List[ArmyAbilityDto],
                        weapons: List[ArmyWeaponDto],
                        items: List[ArmyItemDto],
                        allowedWeaponTypes: List[String],
                        recon: Int,
                        armySpecial: String,
                        defaultWeapons: List[ArmyWeaponDto])

case class ArmyWeaponDto(name: String,
                         points: Int,
                         shootRange: Int,
                         armorPircing: Int,
                         victoryPoints: Int,
                         abilities: List[ArmyAbilityDto],
                         free: Boolean,
                         linkedName: String)

case class ArmyItemDto(name: String, points: Int, rarity: String, noUpdate: Boolean)

case class ArmyTroopWeaponsItemsDto(weapons: Map[String, List[ArmyWeaponDto]], items: List[ArmyItemDto], troop: ArmyTroopDto)

case class ArmyPdfInfos(abilities: List[String], items: List[String], reconVals: List[(String, Int, String)])