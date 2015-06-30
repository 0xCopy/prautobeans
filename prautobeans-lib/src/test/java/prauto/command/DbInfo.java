package prauto.command;

import prauto.ann.ProtoNumber;
import prauto.ann.ProtoOrigin;


@ProtoOrigin("prauto.command.DbInfo")
public interface DbInfo {

	@ProtoNumber(1)
	String	getDb();

}
