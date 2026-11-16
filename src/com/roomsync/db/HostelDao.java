package com.roomsync.db;

import com.roomsync.model.Hostel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reads hostels. */
public class HostelDao {

    public List<Hostel> findAll() throws SQLException {
        List<Hostel> list = new ArrayList<>();
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT hostel_id, hostel_name, score_type, cutoff, occupancy_options FROM HOSTEL ORDER BY hostel_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Hostel(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4), rs.getString(5)));
            }
        }
        return list;
    }
}
