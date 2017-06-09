# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table ability (
  id                            bigint auto_increment not null,
  name                          varchar(255) not null,
  has_inc_val                   tinyint(1) default 0 not null,
  constraint uq_ability_name unique (name),
  constraint pk_ability primary key (id)
);

create table def_troop_ability (
  id                            bigint auto_increment not null,
  troop_id                      bigint,
  ability_id                    bigint,
  default_value                 integer not null,
  constraint pk_def_troop_ability primary key (id)
);

create table def_weapon_ability (
  id                            bigint auto_increment not null,
  weapon_do_id                  bigint,
  ability_id                    bigint,
  default_value                 integer not null,
  constraint pk_def_weapon_ability primary key (id)
);

create table faction (
  id                            bigint auto_increment not null,
  name                          varchar(255) not null,
  constraint uq_faction_name unique (name),
  constraint pk_faction primary key (id)
);

create table troop (
  id                            bigint auto_increment not null,
  name                          varchar(255) not null,
  points                        integer not null,
  model_type                    varchar(255) not null,
  speed                         integer not null,
  sprint                        integer not null,
  shoot                         integer not null,
  fight                         integer not null,
  survive                       integer not null,
  size                          integer not null,
  armour                        integer not null,
  victory_points                integer not null,
  faction_id                    bigint,
  constraint pk_troop primary key (id)
);

create table def_troop_weapon (
  troop_id                      bigint not null,
  weapon_id                     bigint not null,
  constraint pk_def_troop_weapon primary key (troop_id,weapon_id)
);

create table weapon (
  id                            bigint auto_increment not null,
  name                          varchar(255) not null,
  faction_id                    bigint not null,
  weapon_type                   varchar(255),
  weapon_sub_type               varchar(255),
  victory_points                integer not null,
  points                        integer not null,
  shoot_range                   integer not null,
  armor_pircing                 integer not null,
  hart_points                   integer not null,
  free                          tinyint(1) default 0 not null,
  constraint pk_weapon primary key (id)
);

alter table def_troop_ability add constraint fk_def_troop_ability_troop_id foreign key (troop_id) references troop (id) on delete restrict on update restrict;
create index ix_def_troop_ability_troop_id on def_troop_ability (troop_id);

alter table def_troop_ability add constraint fk_def_troop_ability_ability_id foreign key (ability_id) references ability (id) on delete restrict on update restrict;
create index ix_def_troop_ability_ability_id on def_troop_ability (ability_id);

alter table def_weapon_ability add constraint fk_def_weapon_ability_weapon_do_id foreign key (weapon_do_id) references weapon (id) on delete restrict on update restrict;
create index ix_def_weapon_ability_weapon_do_id on def_weapon_ability (weapon_do_id);

alter table def_weapon_ability add constraint fk_def_weapon_ability_ability_id foreign key (ability_id) references ability (id) on delete restrict on update restrict;
create index ix_def_weapon_ability_ability_id on def_weapon_ability (ability_id);

alter table troop add constraint fk_troop_faction_id foreign key (faction_id) references faction (id) on delete restrict on update restrict;
create index ix_troop_faction_id on troop (faction_id);

alter table def_troop_weapon add constraint fk_def_troop_weapon_troop foreign key (troop_id) references troop (id) on delete restrict on update restrict;
create index ix_def_troop_weapon_troop on def_troop_weapon (troop_id);

alter table def_troop_weapon add constraint fk_def_troop_weapon_weapon foreign key (weapon_id) references weapon (id) on delete restrict on update restrict;
create index ix_def_troop_weapon_weapon on def_troop_weapon (weapon_id);

alter table weapon add constraint fk_weapon_faction_id foreign key (faction_id) references faction (id) on delete restrict on update restrict;
create index ix_weapon_faction_id on weapon (faction_id);


# --- !Downs

alter table def_troop_ability drop foreign key fk_def_troop_ability_troop_id;
drop index ix_def_troop_ability_troop_id on def_troop_ability;

alter table def_troop_ability drop foreign key fk_def_troop_ability_ability_id;
drop index ix_def_troop_ability_ability_id on def_troop_ability;

alter table def_weapon_ability drop foreign key fk_def_weapon_ability_weapon_do_id;
drop index ix_def_weapon_ability_weapon_do_id on def_weapon_ability;

alter table def_weapon_ability drop foreign key fk_def_weapon_ability_ability_id;
drop index ix_def_weapon_ability_ability_id on def_weapon_ability;

alter table troop drop foreign key fk_troop_faction_id;
drop index ix_troop_faction_id on troop;

alter table def_troop_weapon drop foreign key fk_def_troop_weapon_troop;
drop index ix_def_troop_weapon_troop on def_troop_weapon;

alter table def_troop_weapon drop foreign key fk_def_troop_weapon_weapon;
drop index ix_def_troop_weapon_weapon on def_troop_weapon;

alter table weapon drop foreign key fk_weapon_faction_id;
drop index ix_weapon_faction_id on weapon;

drop table if exists ability;

drop table if exists def_troop_ability;

drop table if exists def_weapon_ability;

drop table if exists faction;

drop table if exists troop;

drop table if exists def_troop_weapon;

drop table if exists weapon;

