package pl.epodr.sk.womi;

import java.util.Set;

import pl.epodr.sk.IdAndVersion;

public interface IndexDatabase {

	public abstract Set<IdAndVersion> getCollectionsForWomi(long womiId);

	public abstract void putCollectionForWomi(IdAndVersion coll, long womiId);

	public abstract void removeCollection(IdAndVersion coll);

	public abstract void commit();

}