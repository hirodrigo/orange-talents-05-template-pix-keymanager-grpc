package br.com.zup.pix.registra

import br.com.zup.pix.TipoChave
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

internal class TipoChaveTest {

    @Nested
    inner class ALEATORIA {
        @Test
        fun `deve ser valido quando chave aleatoria for nula ou vazia`() {
            with(TipoChave.ALEATORIA) {
                assertTrue(valida(null))
                assertTrue(valida(""))
            }
        }

        @Test
        fun `nao deve ser valido quando chave aleatoria possuir um valor`() {
            with(TipoChave.ALEATORIA) {
                assertFalse(valida(UUID.randomUUID().toString()))
            }
        }
    }

    @Nested
    inner class CPF {
        @Test
        fun `deve ser valido quando cpf tiver numero valido`() {
            with(TipoChave.CPF) {
                assertTrue(valida("85109841071"))
            }
        }

        @Test
        fun `nao deve ser valido quando cpf tiver numero invalido`() {
            with(TipoChave.CPF) {
                assertFalse(valida("85109841072"))
                assertFalse(valida("85109841071a"))
            }
        }

        @Test
        fun `nao deve ser valido quando cpf tiver numero nulo ou vazio`() {
            with(TipoChave.CPF) {
                assertFalse(valida(null))
                assertFalse(valida(""))
            }
        }
    }

    @Nested
    inner class CELULAR {

        @Test
        fun `deve ser valido quando celular for numero valido`() {
            with(TipoChave.CELULAR) {
                assertTrue(valida("+5511987651234"))
            }
        }

        @Test
        fun `nao deve ser valido quando celular for numero invalido`() {
            with(TipoChave.CELULAR) {
                assertFalse(valida("11987651234"))
                assertFalse(valida("119876b51234"))
            }
        }

        @Test
        fun `nao deve ser valido quando celular tiver numero nulo ou vazio`() {
            with(TipoChave.CELULAR) {
                assertFalse(valida(null))
                assertFalse(valida(""))
            }
        }
    }

    @Nested
    inner class EMAIL {

        @Test
        fun `deve ser valido quando email for endereco valido`() {
            with(TipoChave.EMAIL) {
                assertTrue(valida("email@teste.com"))
            }
        }

        @Test
        fun `nao deve ser valido quando email for endereco invalido`() {
            with(TipoChave.EMAIL) {
                assertFalse(valida("emailteste.com"))
                assertFalse(valida("email@teste.com."))
            }
        }

        @Test
        fun `nao deve ser valido quando email for nulo ou vazio`() {
            with(TipoChave.EMAIL) {
                assertFalse(valida(null))
                assertFalse(valida(""))
            }
        }
    }


}