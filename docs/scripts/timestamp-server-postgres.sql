drop table if exists timestamp cascade;

create table if not exists timestamp
(
	serial_number bigint not null
		constraint timestamp_pkey
			primary key,
	date_created timestamp,
	last_update timestamp,
	meta_inf text,
	state varchar(255),
	token_bytes bytea
)
;
