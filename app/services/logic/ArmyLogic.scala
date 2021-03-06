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
    * @param army     the army itself
    * @return
    */
  def changeNameOfArmy(armyName: String, army: ArmyDto): ArmyDto = {
    army.copy(name = armyName.trim)
  }

  /**
    * Adds a new troop to the army
    *
    * @param factionName the name of the faction
    * @param troopName   the name of the troop to add
    * @param army        the army where to add the troop to
    * @return
    */
  def addTroopToArmy(factionName: String, troopName: String, army: ArmyDto): ArmyDto = {
    val troopDo = TroopDAO.findByFactionAndName(factionName, troopName)
    val newTroop = troopDoToArmyAmountTroopDto(troopDo.get)
    val newTroops: List[ArmyAmountTroopDto] = army.troopsWithAmount :+ newTroop
    val factions = getFactionsFromArmy(newTroops)
    setNewTroopsAndFactionAtArmy(army, newTroops, factions)
  }

  /**
    * Collects all factions from the troops as a a comma separated string
    *
    * @param troops get all factions from the given troops
    * @return
    */
  def getFactionsFromArmy(troops: List[ArmyAmountTroopDto]): String = {
    troops.map(_.troop.faction).distinct.mkString(",")
  }

  /**
    * Removes the given troop from the army
    *
    * @param uuid the uuid of the troop to remove from the army
    * @param army the army containing the troop
    * @return
    */
  def removeTroopFromArmy(uuid: String, army: ArmyDto): ArmyDto = {
    val newTroops = army.troopsWithAmount.filter(_.troop.uuid != uuid)
    val faction = if (newTroops.isEmpty) "" else getFactionsFromArmy(newTroops)
    setNewTroopsAndFactionAtArmy(army, newTroops, faction)
  }


  /**
    * Updates the amount of troops in the army by the troop with the uuid
    *
    * @param uuid      the uuid of the troop where to change the amount
    * @param newAmount the new amount to set at the troop
    * @param army      the army containing the troop
    * @return
    */
  def setNewAmountOnTroop(uuid: String, newAmount: Int, army: ArmyDto): ArmyDto = {
    val newTroops = army.troopsWithAmount.map(amountTropp => {
      // it is the troop where to update the amount
      if (amountTropp.troop.uuid == uuid) {
        amountTropp.copy(amount = newAmount)
      } else {
        // dont touch it
        amountTropp
      }
    })

    setNewTroopsAtArmy(army, newTroops)
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
    val newTroops = army.troopsWithAmount :+ troopToClone.copy(troop = troopToClone.troop.copy(uuid = UUID.randomUUID().toString))
    setNewTroopsAtArmy(army, newTroops)
  }

  /**
    * Updates the troop with the given items and weapons
    *
    * @param uuid    the uuid of the troop
    * @param army    the army where to update the troop at
    * @param weapons thr weapons the troop has
    * @param items   the items the troop has
    * @return
    */
  def updateTroop(uuid: String, army: ArmyDto, weapons: List[String], items: List[String]): ArmyDto = {
    val currentTroop = getTroopFromArmy(uuid, army)

    // find the weapons
    val newWeapons = weapons
      .map(weaponName => {
        val weaponDo = WeaponDAO.findByNameAndFactionNameAndAllowedTypes(weaponName, currentTroop.troop.faction, currentTroop.troop.allowedWeaponTypes)
        weaponDoToWeaponDto(weaponDo.get)
      })
      // remove weapons which ared multiple times send by the frontend but have a linkedname
      // For example: Dr. Lukas Koyner has two Anaesthetic Injectors when submitting the fronted edit modal we get two og those
      .groupBy(weapon => {
        weapon.name + weapon.linkedName
      }).
      map(_._2.head)
      .toList


    val newItems = items.map(itemName => {
      val itemDo = ItemDAO.findByNameAndFactionName(itemName, currentTroop.troop.faction)
      itemDoToItemDto(itemDo.get)
    })


    val points = currentTroop.troop.baseStats.basePoints + newWeapons.map(_.points).sum + newItems.map(_.points).sum
    val victoryPoints = currentTroop.troop.baseStats.baseVictoryPoints + newWeapons.map(_.victoryPoints).sum

    val newTroops = army.troopsWithAmount.map(amountTroop => {
      if (amountTroop.troop.uuid != uuid) amountTroop else {
        // add all items which are in the old troop and no upgrade item
        val itemsToSet = newItems ++ amountTroop.troop.items.filter(_.noUpdate == true)
        amountTroop.copy(troop = amountTroop.troop.copy(

          baseStats = amountTroop.troop.baseStats.copy(
            points = points,
            victoryPoints = victoryPoints
          ),
          weapons = newWeapons,
          items = itemsToSet))


      }
    })

    setNewTroopsAtArmy(army, newTroops)
  }

  /**
    * Copies the given army and calculates the points for this army and sets the new faction at the army
    *
    * @param origArmy   the original army
    * @param newTroops  the new troops to seat at the army
    * @param newFaction the name of the new faction of the army
    * @return
    */
  private def setNewTroopsAndFactionAtArmy(origArmy: ArmyDto, newTroops: List[ArmyAmountTroopDto], newFaction: String): ArmyDto = {
    setNewTroopsAtArmy(origArmy, newTroops)
      .copy(faction = newFaction)
  }

  /**
    * Copies the given army and calculates the points for this army
    *
    * @param origArmy  the original army
    * @param newTroops the new troops to seat at the army
    * @return
    */
  private def setNewTroopsAtArmy(origArmy: ArmyDto, newTroops: List[ArmyAmountTroopDto]): ArmyDto = {
    val armyPoints = calculateArmyPoints(newTroops)
    origArmy.copy(points = armyPoints, troopsWithAmount = newTroops)
  }

  /**
    * Calculates the total sum of the troops for the army
    *
    * @param troops the troops where to calculate the sum on
    * @return
    */
  def calculateArmyPoints(troops: List[ArmyAmountTroopDto]): Int = {
    troops.map(amountTroop => amountTroop.troop.baseStats.points * amountTroop.amount).sum
  }

  /**
    * Converts the given troopDo to a [[ArmyAmountTroopDto]]
    *
    * @param troopDo the troop do to convert
    * @return
    */
  def troopDoToArmyAmountTroopDto(troopDo: TroopDO): ArmyAmountTroopDto = {
    ArmyAmountTroopDto(troopDoToArmyTroopDto(troopDo), 1)
  }

  /**
    * Converts the troop database object to the dto
    *
    * @param troopDo the troop to convert to the dto
    * @return
    */
  def troopDoToArmyTroopDto(troopDo: TroopDO): ArmyTroopDto = {

    val troopAbilities = troopDo.defaultTroopAbilities.map(abilityDo => ArmyAbilityDto(abilityDo.ability.name, abilityDo.defaultValue))
    val weapons = troopDo.defaultWeapons.map(weaponDoToWeaponDto)

    val points = troopDo.soldierDto.points + troopDo.defaultWeapons.map(_.points).sum + troopDo.defaultItems.map(_.points).sum
    val victoryPoints = troopDo.soldierDto.victoryPoints + troopDo.defaultWeapons.map(_.victoryPoints).sum

    val weaponTypes = troopDo.allowedWeaponTypes.map(_.name)

    val items = troopDo.defaultItems.map(itemDoToItemDto)


    val baseStats = ArmyTroopBaseStatsDto(
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
      hardPoints = troopDo.soldierDto.hardPoints
    )

    ArmyTroopDto(
      uuid = UUID.randomUUID().toString,
      faction = troopDo.faction.name,
      name = troopDo.soldierDto.name,
      modelType = troopDo.soldierDto.soldierType.toString,
      baseStats = baseStats,
      abilities = troopAbilities,
      weapons = weapons,
      items = items,
      allowedWeaponTypes = weaponTypes,
      recon = troopDo.soldierDto.recon,
      armySpecial = troopDo.soldierDto.armySpecial,
      defaultWeapons = weapons,
      imageUrl = troopDo.soldierDto.imageUrl)
  }

  /**
    * Gets the weapons and items which are allowed for the given uui troop
    * Also returns the currently selected items and weapons
    *
    * @param uuid the uuid of the troop
    * @param army the army where the troop is in
    * @return
    */
  def getWeaponsAndItemsForTroop(uuid: String, army: ArmyDto): ArmyTroopWeaponsItemsDto = {

    val amountTroopDto = getTroopFromArmy(uuid, army)
    val weapons = getWeaponsForTroop(amountTroopDto.troop)
    val items = getItemsForTroop(amountTroopDto.troop)

    ArmyTroopWeaponsItemsDto(weapons, items, amountTroopDto.troop)
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
    * @param uuid the uuid of the troop to find in the current army
    * @param army the army where the troop is located at
    */
  private def getTroopFromArmy(uuid: String, army: ArmyDto): ArmyAmountTroopDto = {
    army.troopsWithAmount.find(_.troop.uuid == uuid).get
  }

  /**
    * Gets the items for the given troop
    *
    * @param troop the troop we want the the items from
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
    * @param weaponDo the do which is to be converted to the dto
    * @return
    */
  def weaponDoToWeaponDto(weaponDo: WeaponDO): ArmyWeaponDto = {
    val abilities = weaponDo.defaultWeaponAbilities.map(abilityDo => ArmyAbilityDto(abilityDo.abilityDO.name, abilityDo.defaultValue))

    ArmyWeaponDto(name = weaponDo.name,
      points = weaponDo.points,
      shootRange = weaponDo.shootRange,
      armorPircing = weaponDo.armorPircing,
      victoryPoints = weaponDo.victoryPoints,
      abilities = abilities,
      free = weaponDo.free,
      linkedName = weaponDo.linkedName,
      hardPoints = weaponDo.hartPoints)
  }

  /**
    * Extracts informations about the army from it for pdf information's
    *
    * @param army the army which is exported to the pdf
    * @return
    */
  def extractPdfArmyInfos(army: ArmyDto): ArmyPdfInfosDto = {
    val abilitiesBuffer = ListBuffer[String]()
    val itemsBuffer = ListBuffer[String]()

    army.troopsWithAmount.foreach(amountTroop => {
      amountTroop.troop.abilities.foreach(ability => {
        abilitiesBuffer += ability.name
      })

      amountTroop.troop.items.foreach(item => {
        itemsBuffer += item.name
      })

      amountTroop.troop.weapons.foreach(weapon => {
        weapon.abilities.foreach(ability => abilitiesBuffer += ability.name)
      })
    })
    val abilities = abilitiesBuffer.toList.distinct.sortWith(_ < _)

    val items = itemsBuffer.toList.distinct.sortWith(_ < _)

    val reconVals = army.troopsWithAmount.filter(_.troop.recon != 0).map(amountTroop => (amountTroop.troop.name, amountTroop.troop.recon, amountTroop.troop.armySpecial)).distinct.sortWith(_._3 < _._3)

    ArmyPdfInfosDto(abilities, items, reconVals)
  }

}

case class ArmyDto(name: String,
                   faction: String = "",
                   points: Int = 0,
                   troopsWithAmount: List[ArmyAmountTroopDto] = List())


/**
  * An abilitiy
  *
  * @param name       name of the ability
  * @param defaultVal the default value the ability has
  */
case class ArmyAbilityDto(name: String,
                          defaultVal: Int)


/**
  * Dto for holding a troop and how many of this troop are in the army
  *
  * @param troop  the troop itself
  * @param amount how many are in the army
  */
case class ArmyAmountTroopDto(troop: ArmyTroopDto,
                              amount: Int = 1)

/**
  * This is the Dto fo a  troop in an army
  *
  * @param uuid               the uuid of the troop in the army
  * @param faction            the faction the troop belongs to
  * @param name               the name of the troop
  * @param modelType          the type of the model
  * @param baseStats          the basic stats of the model
  * @param abilities          the abilities the model has
  * @param weapons            the weapons the troop is equipped with
  * @param items              the items the troop is equipped with
  * @param allowedWeaponTypes the allowed weapon types the troop may select
  * @param recon              the recon value of the troop
  * @param armySpecial        the army special of the troop
  * @param defaultWeapons     the default weapons load out of the troop
  * @param imageUrl           the url of the image of the troop
  */
case class ArmyTroopDto(uuid: String,
                        faction: String,
                        name: String,
                        modelType: String,
                        baseStats: ArmyTroopBaseStatsDto,
                        abilities: List[ArmyAbilityDto],
                        weapons: List[ArmyWeaponDto],
                        items: List[ArmyItemDto],
                        allowedWeaponTypes: List[String],
                        recon: Int,
                        armySpecial: String,
                        defaultWeapons: List[ArmyWeaponDto],
                        imageUrl: String)

case class ArmyTroopBaseStatsDto(basePoints: Int,
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
                                 hardPoints: Int)

/**
  * A Weapon in the army
  *
  * @param name          the name of the weapon in the army
  * @param points        how many points is the weapon worth in the army
  * @param shootRange    how far can this weapon shoot
  * @param armorPircing  how many armor pircing does this weapon have
  * @param victoryPoints how many victory points does this weapon adds to the troop
  * @param hardPoints    how many hardpoints does this weapon require
  * @param abilities     abilities this weapon has
  * @param free          when true this weapon can be added free
  * @param linkedName    when set the weapon must be used with the other weapons with the same linked name
  */
case class ArmyWeaponDto(name: String,
                         points: Int,
                         shootRange: Int,
                         armorPircing: Int,
                         victoryPoints: Int,
                         hardPoints: Int,
                         abilities: List[ArmyAbilityDto],
                         free: Boolean,
                         linkedName: String)

/**
  * This is the dto for holding an army item
  *
  * @param name     the name of the item
  * @param points   how many points this item is worth
  * @param rarity   how rare is this item
  * @param noUpdate when true this item cannot be added or removed from a troop
  */
case class ArmyItemDto(name: String,
                       points: Int,
                       rarity: String,
                       noUpdate: Boolean)

case class ArmyTroopWeaponsItemsDto(weapons: Map[String, List[ArmyWeaponDto]], items: List[ArmyItemDto], troop: ArmyTroopDto)

case class ArmyPdfInfosDto(abilities: List[String],
                           items: List[String],
                           reconVals: List[(String, Int, String)])