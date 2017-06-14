package services.logic

import java.util.UUID

import models._

import scala.collection.JavaConversions._

/**
  * Created by tuxburner on 12.06.17.
  */
object ArmyLogic {


  def addTroopToArmy(factionName: String, troopName: String, army: ArmyDto): ArmyDto = {
    val troopDo = TroopDAO.findByFactionAndName(factionName, troopName)

    val newTroop = troopDoToArmyTroopDto(troopDo)

    val newTroops: List[ArmyTroopDto] = army.troops :+ newTroop
    val armyPoints = newTroops.map(_.points).sum

    army.copy(troops = newTroops,faction = factionName, points = armyPoints)
  }

  def removeTroopFromArmy(uuid: String, army: ArmyDto): ArmyDto = {
    val newTroops = army.troops.filter(_.uuid != uuid)
    val armyPoints = newTroops.map(_.points).sum
    val faction = if(newTroops.size == 0) "" else army.faction
    army.copy(troops = newTroops, faction = faction, points = armyPoints)
  }

  def updateTroop(uuid:String, army:ArmyDto, weapons:List[String], items: List[String]) : ArmyDto = {
    val currentTroop = getTroopFromArmy(uuid,army)
    val newWeapons = weapons.map(weaponName => {
      val weaponDo = WeaponDAO.findByNameAndFactionName(weaponName,currentTroop.faction)
      weaponDoToWeaponDto(weaponDo)
    })

    val newItems = items.map(itemName => {
      val itemDo = ItemDAO.findByNameAndFactionName(itemName,currentTroop.faction)
      itemDoToItemDto(itemDo)
    })

    val points = currentTroop.basePoints + newWeapons.map(_.points).sum




    val newTroops = army.troops.map(troop => {
      if(troop.uuid != uuid) troop
      else {
        troop.copy(points=points,weapons=newWeapons,items=newItems)
      }
    })

    val armyPoints = newTroops.map(_.points).sum

    army.copy(points = armyPoints,troops = newTroops)
  }

  def troopDoToArmyTroopDto(troopDo: TroopDO): ArmyTroopDto = {

    val troopAbilities = troopDo.defaultTroopAbilities.toList.map(abilityDo => ArmyAbilityDto(abilityDo.ability.name, abilityDo.defaultValue))
    val weapons = troopDo.defaultWeapons.toList.map(weaponDoToWeaponDto(_))

    val points = troopDo.points + troopDo.defaultWeapons.toList.map(_.points).sum
    val victoryPoints = troopDo.victoryPoints + troopDo.defaultWeapons.toList.map(_.victoryPoints).sum

    val weaponTypes = troopDo.allowedWeaponTypes.map(_.name).toList

    val items = troopDo.defaultItems.map(itemDoToItemDto(_)).toList

    val uuid = UUID.randomUUID().toString

    ArmyTroopDto(uuid,
      troopDo.faction.name,
      troopDo.name,
      troopDo.modelType,
      troopDo.points,
      points,
      victoryPoints,
      troopDo.speed,
      troopDo.sprint,
      troopDo.armour,
      troopDo.size,
      troopDo.shoot,
      troopDo.fight,
      troopDo.survive,
      troopAbilities,
      weapons,
      items,
      weaponTypes,
      troopDo.recon,
      troopDo.armySpecial)
  }

  /**
    * Gets the weapons and items which are allowed for the given uui troop
    * Also returns the currently selected items and weapons
    * @param uuid
    * @param army
    * @return
    */
  def getWeaponsAndItemsForTroop(uuid: String, army: ArmyDto) : ArmyTroopWeaponsItemsDto = {

    val troopDto = getTroopFromArmy(uuid,army)
    val weapons = getWeaponsForTroop(troopDto)
    val items = getItemsForTroop(army)

    val currentTroopWeapons = troopDto.weapons.map(_.name)
    val currentTroopItems = troopDto.items.map(_.name)

    ArmyTroopWeaponsItemsDto(weapons,items,currentTroopWeapons, currentTroopItems)
  }


  /**
    * Gets all avaible weapon options for the given troop
    * @param troopDto the troop which the weapons are for
    * @return
    */
  def getWeaponsForTroop(troopDto: ArmyTroopDto): Map[String,List[ArmyWeaponDto]] = {
    val weapons = WeaponDAO.findByFactionAndTypes(troopDto.faction, troopDto.allowedWeaponTypes).toList

    val rangedWeaopns = weapons.filter(weaponDo => weaponDo.shootRange != 0 && weaponDo.free == false).map(weaponDoToWeaponDto(_))
    val fightWeapons = weapons.filter(weaponDo => weaponDo.shootRange == 0 && weaponDo.free == false).map(weaponDoToWeaponDto(_))
    val freeWeapons = weapons.filter(_.free == true).map(weaponDoToWeaponDto(_))


    Map("ranged" -> rangedWeaopns, "fight" -> fightWeapons, "free" -> freeWeapons)
  }

  /**
    * Gets the troop from the given army by its uuid
    * @param uuid
    * @param army
    */
  private def getTroopFromArmy(uuid:String, army: ArmyDto): ArmyTroopDto = {
    army.troops.find(_.uuid == uuid).get
  }

  /**
    * Gets the items for the given troop
    * @param army
    * @return
    */
  def getItemsForTroop(army: ArmyDto) : List[ArmyItemDto] = {
    ItemDAO.findAllItemsForFaction(army.faction).map(itemDoToItemDto(_))
  }

  /**
    * Transforms an item from the backend to an item for the frontend.
    * @param itemDo
    * @return
    */
  def itemDoToItemDto(itemDo: ItemDO) : ArmyItemDto = {
    ArmyItemDto(itemDo.name, itemDo.points, itemDo.rarity)
  }

  /**
    * Transforms a weapon database object to a weapon dto
    * @param weaponDo
    * @return
    */
  def weaponDoToWeaponDto(weaponDo: WeaponDO): ArmyWeaponDto = {
    val abilities = weaponDo.defaultWeaponAbilities.toList.map(abilityDo => ArmyAbilityDto(abilityDo.ability.name, abilityDo.defaultValue))
    ArmyWeaponDto(weaponDo.name,
      weaponDo.points,
      weaponDo.shootRange,
      weaponDo.armorPircing,
      weaponDo.victoryPoints,
      abilities)
  }

}

case class ArmyDto(name: String, faction:String = "", points: Int = 0, troops: List[ArmyTroopDto] = List())

case class ArmyAbilityDto(name: String, defaultVal: Int)

case class ArmyTroopDto(uuid: String,
                        faction: String,
                        name: String,
                        modelType: String,
                        basePoints: Int,
                        points: Int,
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
                        armySpecial: String)

case class ArmyWeaponDto(name: String,
                         points: Int,
                         shootRange: Int,
                         armorPircing: Int,
                         victoryPoints: Int,
                         abilities: List[ArmyAbilityDto])

case class ArmyItemDto(name: String, points: Int, rarity: String)

case class ArmyTroopWeaponsItemsDto(weapons: Map[String,List[ArmyWeaponDto]], items: List[ArmyItemDto], currentWeapons: List[String], currentItems: List[String])