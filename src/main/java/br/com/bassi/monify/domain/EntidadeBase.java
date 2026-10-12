package br.com.bassi.monify.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.proxy.HibernateProxy;

import java.util.UUID;

@MappedSuperclass
@Getter
public abstract class EntidadeBase {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Override
    public final boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (outro == null || classeReal(this) != classeReal(outro)) {
            return false;
        }
        var entidade = (EntidadeBase) outro;
        return id != null && id.equals(entidade.getId());
    }

    @Override
    public final int hashCode() {
        return classeReal(this).hashCode();
    }

    private static Class<?> classeReal(Object objeto) {
        return objeto instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : objeto.getClass();
    }
}
