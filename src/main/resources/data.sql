INSERT INTO room_type (id, name, capacity) VALUES (1, '2인실', 2), (2, '4인실', 4), (3, '가족실', 6);

INSERT INTO room (room_no, room_type_id) VALUES
    ('101', 1), ('102', 1), ('103', 1),
    ('201', 2), ('202', 2),
    ('301', 3);
