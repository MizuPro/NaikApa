import argparse
import csv
import sqlite3
from pathlib import Path


DATASETS = (
    ("tj", "data/gtfs", "Tije", "halte"),
    ("krl", "data/gtfs-krl", "KAIC", "stasiun"),
    ("mrt", "data/gtfs-mrt", "MRTJ", "stasiun"),
    ("lrt", "data/gtfs-lrt", "LRTJ", "stasiun"),
)

DATABASE_VERSION = 1


CREATE_TABLES = (
    """
    CREATE TABLE users (
        id_user INTEGER PRIMARY KEY AUTOINCREMENT,
        nama TEXT NOT NULL,
        email TEXT NOT NULL UNIQUE,
        password TEXT NOT NULL,
        has_motor INTEGER NOT NULL DEFAULT 0,
        has_car INTEGER NOT NULL DEFAULT 0,
        created_at INTEGER NOT NULL
    )
    """,
    """
    CREATE TABLE user_profiles (
        id_profile INTEGER PRIMARY KEY AUTOINCREMENT,
        id_user INTEGER NOT NULL UNIQUE,
        default_mode TEXT,
        default_priority TEXT,
        home_lat REAL,
        home_lon REAL,
        home_label TEXT,
        FOREIGN KEY(id_user) REFERENCES users(id_user) ON DELETE CASCADE
    )
    """,
    """
    CREATE TABLE gtfs_stops (
        stop_id TEXT PRIMARY KEY,
        stop_name TEXT NOT NULL,
        stop_lat REAL NOT NULL,
        stop_lon REAL NOT NULL,
        agency_id TEXT NOT NULL,
        stop_type TEXT
    )
    """,
    """
    CREATE TABLE gtfs_routes (
        route_id TEXT PRIMARY KEY,
        agency_id TEXT NOT NULL,
        route_short_name TEXT,
        route_long_name TEXT,
        route_color TEXT,
        route_text_color TEXT
    )
    """,
    """
    CREATE TABLE gtfs_trips (
        trip_id TEXT PRIMARY KEY,
        route_id TEXT NOT NULL,
        service_id TEXT,
        direction_id INTEGER,
        FOREIGN KEY(route_id) REFERENCES gtfs_routes(route_id) ON DELETE CASCADE
    )
    """,
    """
    CREATE TABLE gtfs_stop_times (
        id_stop_time INTEGER PRIMARY KEY AUTOINCREMENT,
        trip_id TEXT NOT NULL,
        arrival_time TEXT NOT NULL,
        departure_time TEXT NOT NULL,
        stop_id TEXT NOT NULL,
        stop_sequence INTEGER NOT NULL,
        FOREIGN KEY(trip_id) REFERENCES gtfs_trips(trip_id) ON DELETE CASCADE,
        FOREIGN KEY(stop_id) REFERENCES gtfs_stops(stop_id) ON DELETE CASCADE
    )
    """,
    """
    CREATE TABLE saved_trips (
        id_saved INTEGER PRIMARY KEY AUTOINCREMENT,
        id_user INTEGER NOT NULL,
        nama_perjalanan TEXT NOT NULL,
        origin_name TEXT NOT NULL,
        origin_lat REAL NOT NULL,
        origin_lon REAL NOT NULL,
        destination_name TEXT NOT NULL,
        destination_lat REAL NOT NULL,
        destination_lon REAL NOT NULL,
        mode TEXT NOT NULL,
        priority TEXT NOT NULL,
        catatan TEXT,
        created_at INTEGER NOT NULL,
        FOREIGN KEY(id_user) REFERENCES users(id_user) ON DELETE CASCADE
    )
    """,
    """
    CREATE TABLE search_history (
        id_search INTEGER PRIMARY KEY AUTOINCREMENT,
        id_user INTEGER NOT NULL,
        keyword TEXT NOT NULL,
        selected_name TEXT NOT NULL,
        selected_address TEXT,
        selected_lat REAL NOT NULL,
        selected_lon REAL NOT NULL,
        searched_at INTEGER NOT NULL,
        FOREIGN KEY(id_user) REFERENCES users(id_user) ON DELETE CASCADE
    )
    """,
    """
    CREATE TABLE route_history (
        id_history INTEGER PRIMARY KEY AUTOINCREMENT,
        id_user INTEGER NOT NULL,
        origin_name TEXT NOT NULL,
        destination_name TEXT NOT NULL,
        mode TEXT NOT NULL,
        priority TEXT NOT NULL,
        recommendation_summary TEXT NOT NULL,
        score INTEGER NOT NULL,
        estimated_time INTEGER NOT NULL,
        estimated_cost INTEGER NOT NULL,
        estimated_bbm INTEGER NOT NULL,
        walking_distance REAL NOT NULL,
        transit_count INTEGER NOT NULL,
        created_at INTEGER NOT NULL,
        FOREIGN KEY(id_user) REFERENCES users(id_user) ON DELETE CASCADE
    )
    """,
    """
    CREATE TABLE disruption_reports (
        id_report INTEGER PRIMARY KEY AUTOINCREMENT,
        id_user INTEGER NOT NULL,
        stop_id TEXT,
        route_id TEXT,
        category TEXT NOT NULL,
        description TEXT NOT NULL,
        photo_path TEXT,
        impact_level INTEGER NOT NULL DEFAULT 1,
        status TEXT NOT NULL DEFAULT 'active',
        created_at INTEGER NOT NULL,
        expired_at INTEGER NOT NULL,
        FOREIGN KEY(id_user) REFERENCES users(id_user) ON DELETE CASCADE
    )
    """,
    """
    CREATE TABLE route_cache (
        id_cache INTEGER PRIMARY KEY AUTOINCREMENT,
        origin_lat REAL NOT NULL,
        origin_lon REAL NOT NULL,
        destination_lat REAL NOT NULL,
        destination_lon REAL NOT NULL,
        mode TEXT NOT NULL,
        priority TEXT NOT NULL,
        result_json TEXT NOT NULL,
        created_at INTEGER NOT NULL
    )
    """,
)


INDEXES = (
    "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)",
    "CREATE INDEX IF NOT EXISTS idx_saved_trips_user ON saved_trips(id_user)",
    "CREATE INDEX IF NOT EXISTS idx_search_history_user_time ON search_history(id_user, searched_at)",
    "CREATE INDEX IF NOT EXISTS idx_route_history_user_time ON route_history(id_user, created_at)",
    "CREATE INDEX IF NOT EXISTS idx_disruptions_status_expired ON disruption_reports(status, expired_at)",
    "CREATE INDEX IF NOT EXISTS idx_disruptions_stop_route ON disruption_reports(stop_id, route_id)",
    "CREATE INDEX IF NOT EXISTS idx_gtfs_stops_name ON gtfs_stops(stop_name)",
    "CREATE INDEX IF NOT EXISTS idx_gtfs_stops_agency ON gtfs_stops(agency_id)",
    "CREATE INDEX IF NOT EXISTS idx_gtfs_routes_agency ON gtfs_routes(agency_id)",
    "CREATE INDEX IF NOT EXISTS idx_gtfs_trips_route ON gtfs_trips(route_id)",
    "CREATE INDEX IF NOT EXISTS idx_gtfs_stop_times_trip_sequence ON gtfs_stop_times(trip_id, stop_sequence)",
    "CREATE INDEX IF NOT EXISTS idx_gtfs_stop_times_stop ON gtfs_stop_times(stop_id)",
    "CREATE INDEX IF NOT EXISTS idx_route_cache_lookup ON route_cache(origin_lat, origin_lon, destination_lat, destination_lon, mode, priority)",
)


def read_csv(path):
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        yield from csv.DictReader(handle)


def scoped_id(dataset_key, raw_id):
    return f"{dataset_key}:{raw_id.strip()}"


def optional_text(value):
    value = (value or "").strip()
    return value or None


def optional_int(value):
    value = (value or "").strip()
    return int(value) if value else None


def valid_coordinate(lat, lon):
    try:
        lat_value = float(lat)
        lon_value = float(lon)
    except (TypeError, ValueError):
        return None
    if not (-90 <= lat_value <= 90 and -180 <= lon_value <= 180):
        return None
    return lat_value, lon_value


def recreate_schema(conn):
    conn.execute("PRAGMA foreign_keys = OFF")
    for table in (
        "route_cache",
        "disruption_reports",
        "route_history",
        "search_history",
        "saved_trips",
        "gtfs_stop_times",
        "gtfs_trips",
        "gtfs_routes",
        "gtfs_stops",
        "user_profiles",
        "users",
    ):
        conn.execute(f"DROP TABLE IF EXISTS {table}")
    for statement in CREATE_TABLES:
        conn.execute(statement)
    for statement in INDEXES:
        conn.execute(statement)
    conn.execute(f"PRAGMA user_version = {DATABASE_VERSION}")
    conn.execute("PRAGMA foreign_keys = ON")


def import_dataset(conn, project_root, dataset_key, relative_dir, default_agency, stop_type):
    dataset_dir = project_root / relative_dir
    if not dataset_dir.exists():
        raise FileNotFoundError(f"Missing GTFS directory: {dataset_dir}")

    route_ids = set()
    trip_ids = set()
    stop_ids = set()
    skipped_stops = 0
    skipped_stop_times = 0

    for row in read_csv(dataset_dir / "stops.txt"):
        raw_stop_id = optional_text(row.get("stop_id"))
        stop_name = optional_text(row.get("stop_name"))
        coord = valid_coordinate(row.get("stop_lat"), row.get("stop_lon"))
        if not raw_stop_id or not stop_name or coord is None:
            skipped_stops += 1
            continue
        stop_id = scoped_id(dataset_key, raw_stop_id)
        stop_ids.add(stop_id)
        conn.execute(
            """
            INSERT OR REPLACE INTO gtfs_stops
            (stop_id, stop_name, stop_lat, stop_lon, agency_id, stop_type)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (stop_id, stop_name, coord[0], coord[1], default_agency, stop_type),
        )

    for row in read_csv(dataset_dir / "routes.txt"):
        raw_route_id = optional_text(row.get("route_id"))
        if not raw_route_id:
            continue
        route_id = scoped_id(dataset_key, raw_route_id)
        route_ids.add(route_id)
        conn.execute(
            """
            INSERT OR REPLACE INTO gtfs_routes
            (route_id, agency_id, route_short_name, route_long_name, route_color, route_text_color)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (
                route_id,
                optional_text(row.get("agency_id")) or default_agency,
                optional_text(row.get("route_short_name")),
                optional_text(row.get("route_long_name")),
                optional_text(row.get("route_color")),
                optional_text(row.get("route_text_color")),
            ),
        )

    for row in read_csv(dataset_dir / "trips.txt"):
        raw_trip_id = optional_text(row.get("trip_id"))
        raw_route_id = optional_text(row.get("route_id"))
        if not raw_trip_id or not raw_route_id:
            continue
        route_id = scoped_id(dataset_key, raw_route_id)
        if route_id not in route_ids:
            continue
        trip_id = scoped_id(dataset_key, raw_trip_id)
        trip_ids.add(trip_id)
        conn.execute(
            """
            INSERT OR REPLACE INTO gtfs_trips
            (trip_id, route_id, service_id, direction_id)
            VALUES (?, ?, ?, ?)
            """,
            (
                trip_id,
                route_id,
                optional_text(row.get("service_id")),
                optional_int(row.get("direction_id")),
            ),
        )

    stop_time_rows = []
    for row in read_csv(dataset_dir / "stop_times.txt"):
        raw_trip_id = optional_text(row.get("trip_id"))
        raw_stop_id = optional_text(row.get("stop_id"))
        arrival_time = optional_text(row.get("arrival_time"))
        departure_time = optional_text(row.get("departure_time"))
        stop_sequence = optional_int(row.get("stop_sequence"))
        if not raw_trip_id or not raw_stop_id or not arrival_time or not departure_time or stop_sequence is None:
            skipped_stop_times += 1
            continue
        trip_id = scoped_id(dataset_key, raw_trip_id)
        stop_id = scoped_id(dataset_key, raw_stop_id)
        if trip_id not in trip_ids or stop_id not in stop_ids:
            skipped_stop_times += 1
            continue
        stop_time_rows.append((trip_id, arrival_time, departure_time, stop_id, stop_sequence))

    conn.executemany(
        """
        INSERT INTO gtfs_stop_times
        (trip_id, arrival_time, departure_time, stop_id, stop_sequence)
        VALUES (?, ?, ?, ?, ?)
        """,
        stop_time_rows,
    )

    return {
        "dataset": dataset_key,
        "stops": len(stop_ids),
        "routes": len(route_ids),
        "trips": len(trip_ids),
        "stop_times": len(stop_time_rows),
        "skipped_stops": skipped_stops,
        "skipped_stop_times": skipped_stop_times,
    }


def build_database(project_root, output_path):
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    with sqlite3.connect(output_path) as conn:
        recreate_schema(conn)
        summaries = []
        for dataset in DATASETS:
            with conn:
                summaries.append(import_dataset(conn, project_root, *dataset))
        conn.execute("PRAGMA optimize")
        conn.execute("VACUUM")
    return summaries


def main():
    parser = argparse.ArgumentParser(description="Build NaikApa pre-built GTFS SQLite database.")
    parser.add_argument("--project-root", type=Path, default=Path.cwd())
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/assets/databases/naikapa_gtfs.db"),
    )
    args = parser.parse_args()

    project_root = args.project_root.resolve()
    output_path = args.output
    if not output_path.is_absolute():
        output_path = project_root / output_path

    summaries = build_database(project_root, output_path)
    print(f"Built {output_path}")
    for summary in summaries:
        print(
            "{dataset}: stops={stops}, routes={routes}, trips={trips}, "
            "stop_times={stop_times}, skipped_stops={skipped_stops}, "
            "skipped_stop_times={skipped_stop_times}".format(**summary)
        )


if __name__ == "__main__":
    main()
