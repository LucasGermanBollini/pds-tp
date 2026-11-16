-- Esquema inicial de SkillSwap.
-- Generado a partir del mapeo real de las entidades JPA (Hibernate
-- schema-export en modo "scripts only", sin tocar ninguna base), para
-- garantizar que coincide exactamente con @Entity/@Column/@JoinColumn.
-- A partir de aca el esquema lo versiona Flyway, no hibernate.hbm2ddl.auto
-- (seccion 8 del TP: "no depender de ddl-auto=create en la entrega").

create table perfiles (
    id                       varchar(100) not null,
    nombre_visible           varchar(255) not null,
    biografia                varchar(2000),
    zona_horaria             varchar(255),
    rol_descripcion          varchar(255),
    canal_preferido          enum('EMAIL', 'PUSH', 'INTERNA') not null default 'INTERNA',
    reputacion_promedio      double       not null default 0,
    cantidad_calificaciones  integer      not null default 0,
    activo                   bit          not null default true,
    primary key (id)
) engine = InnoDB;

create table elementos_catalogo (
    id                  varchar(36)  not null,
    tipo_elemento       varchar(31)  not null,
    nombre              varchar(255) not null,
    categoria_padre_id  varchar(36),
    primary key (id)
) engine = InnoDB;

create table perfil_habilidades (
    perfil_id     varchar(100) not null,
    habilidad_id  varchar(36)  not null
) engine = InnoDB;

create table publicaciones (
    id                           varchar(36)  not null,
    autor_perfil_id              varchar(100) not null,
    titulo                       varchar(255) not null,
    descripcion                  varchar(2000),
    categoria_id                 varchar(36),
    modalidad                    enum('REMOTA', 'PRESENCIAL', 'HIBRIDA'),
    duracion_estimada_minutos    integer      not null default 0,
    valor                        bigint,
    estado                       varchar(255) not null,
    contador_solicitudes         integer      not null default 0,
    primary key (id)
) engine = InnoDB;

create table publicacion_habilidades (
    publicacion_id  varchar(36) not null,
    habilidad_id    varchar(36) not null
) engine = InnoDB;

create table franjas_disponibilidad (
    id                  varchar(36) not null,
    publicacion_id      varchar(36),
    fecha_hora_inicio   datetime(6) not null,
    fecha_hora_fin      datetime(6) not null,
    cupo_total          integer     not null,
    cupo_disponible     integer     not null,
    version             bigint,
    primary key (id)
) engine = InnoDB;

create table contrataciones (
    id                    varchar(36)  not null,
    cliente_perfil_id     varchar(100) not null,
    publicacion_id        varchar(36)  not null,
    franja_elegida_id     varchar(36)  not null,
    mensaje               varchar(1000),
    objetivo              varchar(1000),
    estado                varchar(255) not null,
    motivo_cancelacion    varchar(255),
    valor                 bigint,
    fecha_creacion        datetime(6)  not null,
    primary key (id)
) engine = InnoDB;

create table historial_estados (
    id               varchar(36)  not null,
    entidad_tipo     varchar(255) not null,
    entidad_id       varchar(36)  not null,
    estado_anterior  varchar(255),
    estado_nuevo     varchar(255) not null,
    actor_user_id    varchar(255),
    motivo           varchar(255),
    fecha            datetime(6)  not null,
    primary key (id)
) engine = InnoDB;

create table calificaciones (
    id                 varchar(36)  not null,
    contratacion_id    varchar(36)  not null,
    autor_user_id      varchar(255) not null,
    receptor_user_id   varchar(255) not null,
    puntaje            integer      not null,
    comentario         varchar(1000),
    fecha              datetime(6)  not null,
    primary key (id)
) engine = InnoDB;

create table wallets_skillcoins (
    id                varchar(36)  not null,
    titular_user_id   varchar(255) not null,
    valor             bigint,
    primary key (id)
) engine = InnoDB;

create table movimientos_creditos (
    id                varchar(36)  not null,
    wallet_id         varchar(36)  not null,
    tipo              enum('DEBITO', 'CREDITO') not null,
    valor             bigint,
    motivo            varchar(255),
    contratacion_id   varchar(36),
    fecha             datetime(6)  not null,
    saldo_posterior   bigint,
    primary key (id)
) engine = InnoDB;

create table notificaciones (
    id                     varchar(36)  not null,
    destinatario_user_id   varchar(255) not null,
    canal                  enum('EMAIL', 'PUSH', 'INTERNA') not null,
    mensaje                varchar(1000),
    leida                  bit          not null default false,
    evento_origen          varchar(255),
    fecha                  datetime(6)  not null,
    primary key (id)
) engine = InnoDB;

-- restricciones de unicidad
alter table wallets_skillcoins add constraint uk_wallet_titular unique (titular_user_id);

-- claves foraneas
alter table elementos_catalogo add constraint fk_elemento_categoria_padre
    foreign key (categoria_padre_id) references elementos_catalogo (id);

alter table perfil_habilidades add constraint fk_perfil_habilidad_perfil
    foreign key (perfil_id) references perfiles (id);
alter table perfil_habilidades add constraint fk_perfil_habilidad_habilidad
    foreign key (habilidad_id) references elementos_catalogo (id);

alter table publicaciones add constraint fk_publicacion_autor
    foreign key (autor_perfil_id) references perfiles (id);
alter table publicaciones add constraint fk_publicacion_categoria
    foreign key (categoria_id) references elementos_catalogo (id);

alter table publicacion_habilidades add constraint fk_publicacion_habilidad_publicacion
    foreign key (publicacion_id) references publicaciones (id);
alter table publicacion_habilidades add constraint fk_publicacion_habilidad_habilidad
    foreign key (habilidad_id) references elementos_catalogo (id);

alter table franjas_disponibilidad add constraint fk_franja_publicacion
    foreign key (publicacion_id) references publicaciones (id);

alter table contrataciones add constraint fk_contratacion_cliente
    foreign key (cliente_perfil_id) references perfiles (id);
alter table contrataciones add constraint fk_contratacion_publicacion
    foreign key (publicacion_id) references publicaciones (id);
alter table contrataciones add constraint fk_contratacion_franja
    foreign key (franja_elegida_id) references franjas_disponibilidad (id);

-- indices para las consultas principales (seccion 8: "evitar N+1... y justificar fetch plans")
create index idx_publicaciones_estado on publicaciones (estado);
create index idx_contrataciones_estado on contrataciones (estado);
create index idx_historial_entidad on historial_estados (entidad_tipo, entidad_id);
create index idx_notificaciones_destinatario on notificaciones (destinatario_user_id);
create index idx_calificaciones_receptor on calificaciones (receptor_user_id);
create index idx_calificaciones_contratacion on calificaciones (contratacion_id);
