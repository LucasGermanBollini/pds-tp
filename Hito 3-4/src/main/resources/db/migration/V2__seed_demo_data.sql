-- Datos de demostracion reproducibles para desarrollo local (seccion 8).
-- IDs fijos (no UUID random) para que la demo sea siempre la misma.

insert into perfiles (id, nombre_visible, biografia, zona_horaria, reputacion_promedio, cantidad_calificaciones, activo)
values
    ('demo-prof-ada', 'Ada, mentora de carrera', 'Ex-devops, ahora ayuda a preparar entrevistas tecnicas.', 'America/Argentina/Buenos_Aires', 0, 0, true),
    ('demo-cli-beto', 'Beto', 'Busca su primer trabajo de desarrollador.', 'America/Argentina/Buenos_Aires', 0, 0, true);

insert into elementos_catalogo (id, tipo_elemento, nombre, categoria_padre_id)
values
    ('demo-cat-carrera', 'CATEGORIA', 'Carrera', null),
    ('demo-hab-java', 'HABILIDAD', 'Java', null);

insert into publicaciones (id, autor_perfil_id, titulo, descripcion, categoria_id, modalidad,
                            duracion_estimada_minutos, valor, estado, contador_solicitudes)
values
    ('demo-pub-1', 'demo-prof-ada', 'Preparacion para entrevistas tecnicas',
     'Simulacro de entrevista backend + feedback', 'demo-cat-carrera', 'REMOTA', 60, 300, 'PUBLICADA', 0);

insert into publicacion_habilidades (publicacion_id, habilidad_id)
values ('demo-pub-1', 'demo-hab-java');

insert into franjas_disponibilidad (id, publicacion_id, fecha_hora_inicio, fecha_hora_fin, cupo_total, cupo_disponible, version)
values
    ('demo-franja-1', 'demo-pub-1', now() + interval '2' day, now() + interval '2' day + interval '1' hour, 3, 3, 0);

insert into wallets_skillcoins (id, titular_user_id, valor)
values
    ('demo-wallet-ada', 'demo-prof-ada', 0),
    ('demo-wallet-beto', 'demo-cli-beto', 1000);
