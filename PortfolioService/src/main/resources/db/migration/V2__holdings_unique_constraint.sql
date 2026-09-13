-- holdings is the source of truth for every downstream read model, all of which already
-- declare UNIQUE (portfolio_id, symbol). Without the same constraint here, two concurrent
-- first-buys of the same symbol both miss the upsert lookup and both insert; @Version guards
-- concurrent updates to an existing row, not duplicate inserts. Once duplicated, every
-- subsequent buy or sell for that symbol fails with IncorrectResultSizeDataAccessException.
ALTER TABLE holdings
    ADD CONSTRAINT uq_holdings_portfolio_symbol UNIQUE (portfolio_id, symbol);
