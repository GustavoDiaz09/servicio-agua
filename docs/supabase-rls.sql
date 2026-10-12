-- RLS para el sistema de agua en Supabase (proyecto de producción).
-- El esquema lo genera Hibernate (ddl-auto: update): no hay migraciones SQL en el repo.
-- Ejecutar UNA VEZ tras el primer arranque en cada base nueva (SQL Editor de Supabase o psql).
-- RLS sin policies: PostgREST (roles anon/authenticated) queda bloqueado.
-- El backend entra por JDBC como `postgres` (BYPASSRLS), así que no se ve afectado.
-- Resuelve los advisories `rls_disabled_in_public` y `sensitive_columns_exposed` (usuario.password).

DO $$
DECLARE
  r record;
BEGIN
  FOR r IN
    SELECT tablename FROM pg_tables WHERE schemaname = 'public'
  LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY;', r.tablename);
  END LOOP;
END
$$;
