package prauto.command
        ;

import prauto.ann.Optional;
import prauto.ann.ProtoNumber;
import prauto.ann.ProtoOrigin;

import java.util.List;


@ProtoOrigin("prauto.command.CreateDb.ComplexPrautoBean")
public interface ComplexPrautoBean {

    @Optional(value = 1)
    @ProtoNumber(value = 2)
    boolean getAutoCompaction();


    @Optional(2)
    @ProtoNumber(1)
    String getName();


    @Optional(3)
    @ProtoNumber(3)
    String getCache();


    @Optional(4)
    @ProtoNumber(4)
    String getAdapter();

    @Optional(5)
    @ProtoNumber(5)
    List<ComplexPrautoBean> getChallenge();

    @Optional(6)
    @ProtoNumber(6)
    List<String> getChallenge2();

    @Optional(7)
    @ProtoNumber(7)
    List< TheEnum > getEnumThingy();

}
