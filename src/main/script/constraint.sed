# --regexp-extended
/ALTER TABLE/ {
  h
  d
}
/CONSTRAINT/ {
  H
  s~ *CONSTRAINT ([A-Z0-9_)]+) .*~SELECT 'Adding constraint \1 ...' AS Progress, CURRENT_TIME() AS Time;\n~p
  g
}


