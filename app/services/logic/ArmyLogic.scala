package services.logic

import java.util.UUID

import models.{TroopDAO, TroopDO, WeaponDAO, WeaponDO}

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

  def troopDoToArmyTroopDto(troopDo: TroopDO): ArmyTroopDto = {

    val troopAbilities = troopDo.defaultTroopAbilities.toList.map(abilityDo => ArmyAbilityDto(abilityDo.ability.name, abilityDo.defaultValue))
    val weapons = troopDo.defaultEquipment.toList.map(weaponDoToWeaponDto(_))

    val points = troopDo.points + troopDo.defaultEquipment.toList.map(_.points).sum
    val victoryPoints = troopDo.victoryPoints + troopDo.defaultEquipment.toList.map(_.victoryPoints).sum

    val weaponTypes = troopDo.allowedWeaponTypes.map(_.name).toList

    val uuid = UUID.randomUUID().toString

    ArmyTroopDto(uuid,
      troopDo.faction.name,
      troopDo.name,
      troopDo.modelType,
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
      weaponTypes,
      troopDo.recon,
      troopDo.armySpecial)
  }

  def getWeaponsForTroop(uuid: String, army: ArmyDto): List[ArmyWeaponDto] = {
    val troopDto = army.troops.find(_.uuid == uuid).get
    val weapons = WeaponDAO.findByFactionAndTypes(troopDto.faction, troopDto.allowedWeaponTypes)
    weapons.toList.map(weaponDoToWeaponDto(_))
  }

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
                        allowedWeaponTypes: List[String],
                        recon: Int,
                        armySpecial: String)

case class ArmyWeaponDto(name: String,
                         points: Int,
                         shootRange: Int,
                         armorPircing: Int,
                         victoryPoints: Int,
                         abilities: List[ArmyAbilityDto])