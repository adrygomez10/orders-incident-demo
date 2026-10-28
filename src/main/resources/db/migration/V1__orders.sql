CREATE TABLE orders (id uuid PRIMARY KEY,customer_reference varchar(100) NOT NULL,quantity integer NOT NULL CHECK(quantity>0));
