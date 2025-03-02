-- Создание ролей
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'admin_role') THEN
        CREATE ROLE admin_role WITH NOLOGIN CREATEROLE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'guest_role') THEN
        CREATE ROLE guest_role NOLOGIN;
    END IF;
END $$;

-- Создание таблицы
CREATE TABLE IF NOT EXISTS games (
    game_id INTEGER PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    release_date DATE,
    rating DOUBLE PRECISION
);

-- Процедура очистки таблицы
CREATE OR REPLACE PROCEDURE clear_games_table()
AS $$
BEGIN
    TRUNCATE TABLE games;
END;
$$ LANGUAGE plpgsql;

-- Процедура добавления игры
CREATE OR REPLACE PROCEDURE add_game(
    p_game_id INTEGER,
    p_title VARCHAR(255),
    p_release_date DATE,
    p_rating DOUBLE PRECISION
)
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM games WHERE game_id = p_game_id) THEN
        RAISE EXCEPTION 'Game with ID % already exists', p_game_id;
    END IF;
    INSERT INTO games(game_id, title, release_date, rating)
    VALUES (p_game_id, p_title, p_release_date, p_rating);
END;
$$ LANGUAGE plpgsql;

-- Функция поиска по названию
CREATE OR REPLACE FUNCTION search_by_title(search_title VARCHAR(255))
RETURNS TABLE (
    game_id INTEGER,
    title VARCHAR(255),
    release_date DATE,
    rating DOUBLE PRECISION
)
SECURITY INVOKER
AS $$
BEGIN
    RETURN QUERY SELECT g.game_id, g.title, g.release_date, g.rating
                 FROM games g
                 WHERE g.title ILIKE '%' || search_title || '%';
END;
$$ LANGUAGE plpgsql;

-- Процедура обновления игры
CREATE OR REPLACE PROCEDURE update_game(
    p_game_id INTEGER,
    p_title VARCHAR(255),
    p_release_date DATE,
    p_rating DOUBLE PRECISION
)
AS $$
BEGIN
    UPDATE games
    SET title = p_title, release_date = p_release_date, rating = p_rating
    WHERE game_id = p_game_id;
END;
$$ LANGUAGE plpgsql;

-- Процедура удаления по названию
CREATE OR REPLACE PROCEDURE delete_by_title(
    p_title VARCHAR(255),
    OUT rows_deleted INTEGER
)
AS $$
BEGIN
    DELETE FROM games WHERE title = p_title;
    GET DIAGNOSTICS rows_deleted = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

-- Процедура создания пользователя
CREATE OR REPLACE PROCEDURE create_db_user(
    p_username VARCHAR(63),
    p_password VARCHAR(63),
    p_role VARCHAR(20)
)
AS $$
BEGIN
    IF 'admin_role' NOT IN (SELECT rolname FROM pg_roles WHERE pg_has_role(current_user, oid, 'member')) THEN
        RAISE EXCEPTION 'Only administrators can create users';
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = p_username) THEN
        RAISE EXCEPTION 'User % already exists', p_username;
    END IF;

    EXECUTE format('CREATE USER %I WITH LOGIN PASSWORD %L', p_username, p_password);
    IF p_role = 'admin' THEN
        EXECUTE format('GRANT admin_role TO %I WITH ADMIN OPTION', p_username);
    ELSIF p_role = 'guest' THEN
        EXECUTE format('GRANT guest_role TO %I WITH ADMIN OPTION', p_username);
    ELSE
        RAISE EXCEPTION 'Invalid role: %', p_role;
    END IF;
    EXECUTE format('GRANT CONNECT ON DATABASE games_db TO %I', p_username);
END;
$$ LANGUAGE plpgsql;

-- Назначение прав
GRANT ALL ON games TO admin_role;
GRANT SELECT ON games TO guest_role;
GRANT EXECUTE ON PROCEDURE clear_games_table TO admin_role;
GRANT EXECUTE ON PROCEDURE add_game TO admin_role;
GRANT EXECUTE ON FUNCTION search_by_title TO admin_role;
GRANT EXECUTE ON FUNCTION search_by_title TO guest_role;
GRANT EXECUTE ON PROCEDURE update_game TO admin_role;
GRANT EXECUTE ON PROCEDURE delete_by_title TO admin_role;
GRANT EXECUTE ON PROCEDURE create_db_user TO admin_role;
GRANT guest_role TO admin_role WITH ADMIN OPTION;
