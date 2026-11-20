-- Hito final. Cambios de esquema para cubrir lo que faltaba de la consigna.
-- Misma regla que V1: el esquema lo versiona Flyway, nunca Hibernate.

-- 4.1 / 4.2: perfil profesional u organizacion; intereses y disponibilidad.
alter table perfiles add column tipo_perfil enum('PROFESIONAL', 'ORGANIZACION') not null default 'PROFESIONAL';

create table perfil_intereses (
    perfil_id     varchar(100) not null,
    habilidad_id  varchar(36)  not null,
    primary key (perfil_id, habilidad_id)
) engine = InnoDB;
alter table perfil_intereses add constraint fk_perfil_interes_perfil
    foreign key (perfil_id) references perfiles (id);
alter table perfil_intereses add constraint fk_perfil_interes_habilidad
    foreign key (habilidad_id) references elementos_catalogo (id);

create table perfil_disponibilidad (
    perfil_id   varchar(100) not null,
    dia_semana  enum('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') not null,
    hora_desde  time(6) not null,
    hora_hasta  time(6) not null
) engine = InnoDB;
alter table perfil_disponibilidad add constraint fk_perfil_disponibilidad_perfil
    foreign key (perfil_id) references perfiles (id);

-- 4.2 / 4.3: oferta o desafio; fechas para ordenar por "mas reciente".
alter table publicaciones add column tipo_publicacion enum('OFERTA', 'DESAFIO') not null default 'OFERTA';
alter table publicaciones add column fecha_creacion datetime(6) not null default current_timestamp(6);
alter table publicaciones add column fecha_publicacion datetime(6);
update publicaciones set fecha_publicacion = fecha_creacion where estado = 'PUBLICADA';

-- 4.4: reprogramacion de comun acuerdo (una parte propone, la otra acepta).
alter table contrataciones add column franja_propuesta_id varchar(36);
alter table contrataciones add column reprogramacion_propuesta_por varchar(100);
alter table contrataciones add constraint fk_contratacion_franja_propuesta
    foreign key (franja_propuesta_id) references franjas_disponibilidad (id);

-- 4.6: que evento genero la notificacion y sobre que entidad.
alter table notificaciones add column tipo_evento varchar(40);
alter table notificaciones add column entidad_id varchar(36);

-- 5 / 8: restricciones de unicidad.
-- "Solo participantes de una contratacion completada pueden calificarla una vez": ademas de la
-- validacion del caso de uso, la base garantiza una sola calificacion por (contratacion, autor).
alter table calificaciones add constraint uk_calificacion_contratacion_autor unique (contratacion_id, autor_user_id);
-- Las tablas de relacion no admiten la misma habilidad dos veces.
alter table perfil_habilidades add constraint uk_perfil_habilidad unique (perfil_id, habilidad_id);
alter table publicacion_habilidades add constraint uk_publicacion_habilidad unique (publicacion_id, habilidad_id);
-- No puede haber dos categorias/habilidades con el mismo nombre y tipo.
alter table elementos_catalogo add constraint uk_elemento_tipo_nombre unique (tipo_elemento, nombre);

-- 8: indices para las consultas principales.
create index idx_publicaciones_estado_fecha on publicaciones (estado, fecha_publicacion);   -- busqueda / "mas reciente"
create index idx_publicaciones_estado_precio on publicaciones (estado, valor);             -- filtro y orden por precio
create index idx_publicaciones_autor on publicaciones (autor_perfil_id);                   -- "mis publicaciones"
create index idx_publicaciones_categoria on publicaciones (categoria_id);                  -- filtro por categoria
create index idx_perfiles_reputacion on perfiles (reputacion_promedio);                    -- filtro/orden por reputacion
create index idx_contrataciones_cliente on contrataciones (cliente_perfil_id);             -- "mis contrataciones"
create index idx_contrataciones_publicacion on contrataciones (publicacion_id);
create index idx_franjas_publicacion on franjas_disponibilidad (publicacion_id);
create index idx_movimientos_wallet_fecha on movimientos_creditos (wallet_id, fecha);      -- movimientos de una wallet
create index idx_elementos_nombre on elementos_catalogo (nombre);                          -- buscar habilidad/categoria por nombre
