package com.unev.sige.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unev.sige.model.CatalogoHerramientas;
import com.unev.sige.model.Estudiante;
import com.unev.sige.model.HerramientaEscolar;
import com.unev.sige.model.RegistroHerramienta;
import com.unev.sige.model.Rol;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integración contra MySQL local (omitida si no hay base de prueba): guardar, consultar y eliminar registros. */
class HerramientaRegistroDAOIntegracionTest {
    private static final String CLAVE = "Clave2026";
    private final UsuarioDAO usuarios = new UsuarioDAO();
    private final HerramientaRegistroDAO dao = new HerramientaRegistroDAO();

    @BeforeEach
    void preparar() {
        EntornoIntegracion.exigirBaseDePrueba();
    }

    @AfterEach
    void limpiar() throws Exception {
        if (EntornoIntegracion.habilitado()) EntornoIntegracion.limpiar();
    }

    private Estudiante usuarioNuevo() throws Exception {
        Estudiante e = EntornoIntegracion.nuevoEstudiante(EntornoIntegracion.correoUnico());
        usuarios.registrar(e, CLAVE);
        return e;
    }

    private int guardarRiesgo(int idUsuario) throws Exception {
        HerramientaEscolar h = CatalogoHerramientas.buscar("riesgo-desercion").orElseThrow();
        Map<String, String> v = h.valoresIniciales();
        return dao.guardar(idUsuario, h, v, h.ejecutar(v).texto());
    }

    @Test
    void guardaYRecuperaElRegistroConSusDatosEnOrden() throws Exception {
        Estudiante e = usuarioNuevo();
        int id = guardarRiesgo(e.getId());

        RegistroHerramienta r = dao.obtener(id).orElseThrow();
        assertEquals(e.getId(), r.idUsuario());
        assertEquals("riesgo-desercion", r.herramienta());
        assertTrue(r.resultado().contains("Índice de riesgo"));
        assertEquals(3, r.datos().size());
        assertEquals("inasistencia", r.datos().get(0).campo());
        assertEquals(1, dao.listar(e.getId(), false).size());
    }

    @Test
    void losCamposSensiblesNoSeGuardan() throws Exception {
        Estudiante e = usuarioNuevo();
        HerramientaEscolar h = CatalogoHerramientas.buscar("retiro-estudiante").orElseThrow();
        Map<String, String> v = h.valoresIniciales();
        int id = dao.guardar(e.getId(), h, v, h.ejecutar(v).texto());

        RegistroHerramienta r = dao.obtener(id).orElseThrow();
        assertEquals(0, EntornoIntegracion.consultaEntera("SELECT COUNT(*) FROM herramienta_registro_datos WHERE id_registro=" + id
                + " AND (valor LIKE '%4821%' OR valor LIKE '%001-0000002%')"));
        assertTrue(r.datos().stream().anyMatch(d -> d.campo().equals("pin") && d.valor().equals(RegistroHerramienta.NO_SE_GUARDA)));
        assertFalse(r.resultado().contains("4821"));
    }

    @Test
    void cadaUsuarioSoloVeLosSuyosYElAdministradorTodos() throws Exception {
        Estudiante a = usuarioNuevo();
        Estudiante b = usuarioNuevo();
        guardarRiesgo(a.getId());
        guardarRiesgo(b.getId());

        assertEquals(1, dao.listar(a.getId(), false).size());
        assertTrue(dao.listar(a.getId(), true).size() >= 2);
    }

    @Test
    void soloElDuenoOElAdministradorEliminan() throws Exception {
        Estudiante duenio = usuarioNuevo();
        Estudiante intruso = usuarioNuevo();
        int id = guardarRiesgo(duenio.getId());

        assertThrows(PermisoDenegadoException.class, () -> dao.eliminar(id, intruso.getId(), Rol.ESTUDIANTE));
        assertTrue(dao.obtener(id).isPresent());

        assertTrue(dao.eliminar(id, EntornoIntegracion.idAdministrador(), Rol.ADMIN));
        assertTrue(dao.obtener(id).isEmpty());
        assertEquals(0, EntornoIntegracion.consultaEntera("SELECT COUNT(*) FROM herramienta_registro_datos WHERE id_registro=" + id));
        assertEquals(1, EntornoIntegracion.consultaEntera("SELECT COUNT(*) FROM auditoria WHERE accion='HERRAMIENTA_REGISTRO_ELIMINADO' AND id_entidad='" + id + "'"));
        assertFalse(dao.eliminar(id, duenio.getId(), Rol.ESTUDIANTE));
    }

    @Test
    void alEliminarLaCuentaSeBorranSusRegistros() throws Exception {
        Estudiante e = usuarioNuevo();
        String correo = e.getCorreo();
        int id = guardarRiesgo(e.getId());

        new EliminacionUsuarioDAO().eliminarComoAdmin(EntornoIntegracion.idAdministrador(), e.getId(), correo);

        assertEquals(0, EntornoIntegracion.consultaEntera("SELECT COUNT(*) FROM herramienta_registros WHERE id_registro=" + id));
        assertEquals(0, EntornoIntegracion.consultaEntera("SELECT COUNT(*) FROM herramienta_registro_datos WHERE id_registro=" + id));
    }

    @Test
    void noSePuedeGuardarMasDelTopePorUsuario() throws Exception {
        Estudiante e = usuarioNuevo();
        for (int i = 0; i < RegistroHerramienta.MAX_POR_USUARIO; i++) guardarRiesgo(e.getId());
        assertThrows(IllegalStateException.class, () -> guardarRiesgo(e.getId()));
        assertEquals(RegistroHerramienta.MAX_POR_USUARIO, dao.listar(e.getId(), false).size());
        assertEquals(List.of(), List.of());
    }
}
