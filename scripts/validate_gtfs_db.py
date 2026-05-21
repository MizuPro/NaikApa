import argparse
import sqlite3
from pathlib import Path


REQUIRED_TABLES = (
    "gtfs_stops",
    "gtfs_routes",
    "gtfs_trips",
    "gtfs_stop_times",
)

SAMPLE_KEYWORDS = ("Dukuh", "Tangerang", "Blok M")


def scalar(conn, query, args=()):
    return conn.execute(query, args).fetchone()[0]


def validate(database_path):
    failures = []
    with sqlite3.connect(database_path) as conn:
        integrity = scalar(conn, "PRAGMA integrity_check")
        if integrity != "ok":
            failures.append(f"integrity_check failed: {integrity}")

        user_version = scalar(conn, "PRAGMA user_version")
        if user_version != 1:
            failures.append(f"expected user_version=1, got {user_version}")

        counts = {}
        for table in REQUIRED_TABLES:
            counts[table] = scalar(conn, f"SELECT COUNT(*) FROM {table}")
            if counts[table] <= 0:
                failures.append(f"{table} is empty")

        invalid_coords = scalar(
            conn,
            """
            SELECT COUNT(*)
            FROM gtfs_stops
            WHERE stop_lat IS NULL
               OR stop_lon IS NULL
               OR stop_lat < -90
               OR stop_lat > 90
               OR stop_lon < -180
               OR stop_lon > 180
            """,
        )
        if invalid_coords:
            failures.append(f"invalid stop coordinates: {invalid_coords}")

        orphan_trips = scalar(
            conn,
            """
            SELECT COUNT(*)
            FROM gtfs_trips t
            LEFT JOIN gtfs_routes r ON r.route_id = t.route_id
            WHERE r.route_id IS NULL
            """,
        )
        if orphan_trips:
            failures.append(f"trips without route: {orphan_trips}")

        orphan_stop_times = scalar(
            conn,
            """
            SELECT COUNT(*)
            FROM gtfs_stop_times st
            LEFT JOIN gtfs_trips t ON t.trip_id = st.trip_id
            LEFT JOIN gtfs_stops s ON s.stop_id = st.stop_id
            WHERE t.trip_id IS NULL OR s.stop_id IS NULL
            """,
        )
        if orphan_stop_times:
            failures.append(f"stop_times without trip/stop: {orphan_stop_times}")

        samples = {}
        for keyword in SAMPLE_KEYWORDS:
            samples[keyword] = conn.execute(
                """
                SELECT stop_id, stop_name, agency_id
                FROM gtfs_stops
                WHERE stop_name LIKE ?
                ORDER BY stop_name ASC
                LIMIT 5
                """,
                (f"%{keyword}%",),
            ).fetchall()
            if not samples[keyword]:
                failures.append(f"sample search has no result: {keyword}")

    return counts, samples, failures


def main():
    parser = argparse.ArgumentParser(description="Validate NaikApa pre-built GTFS SQLite database.")
    parser.add_argument(
        "database",
        nargs="?",
        type=Path,
        default=Path("app/src/main/assets/databases/naikapa_gtfs.db"),
    )
    args = parser.parse_args()

    database_path = args.database.resolve()
    if not database_path.exists():
        raise SystemExit(f"Database not found: {database_path}")

    counts, samples, failures = validate(database_path)
    print(f"Validated {database_path}")
    for table, count in counts.items():
        print(f"{table}: {count}")
    for keyword, rows in samples.items():
        print(f"sample {keyword}: {rows[:3]}")

    if failures:
        print("FAILURES:")
        for failure in failures:
            print(f"- {failure}")
        raise SystemExit(1)

    print("Validation OK")


if __name__ == "__main__":
    main()
