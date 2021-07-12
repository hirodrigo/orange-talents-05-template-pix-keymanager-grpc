package br.com.zup.pix.registra

import br.com.zup.pix.ChavePix
import br.com.zup.pix.ContaAssociada
import br.com.zup.pix.TipoChave
import br.com.zup.pix.TipoConta
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class ChavePixTest {

    companion object {
        val TIPOS_DE_CHAVE_EXCETO_ALEATORIO = TipoChave.values().filterNot { it == TipoChave.ALEATORIA }
    }

    @Test
    fun `deve chave ser do tipo aleatorio`() {
        val chavePix = novaChave(TipoChave.ALEATORIA)
        with(chavePix) {
            assertTrue(this.isChaveAleatoria())
        }
    }

    @Test
    fun `nao deve chave ser do tipo aleatorio`() {
        TIPOS_DE_CHAVE_EXCETO_ALEATORIO.forEach { tipoChave ->
            val chavePix = novaChave(tipo = tipoChave)
            assertFalse(chavePix.isChaveAleatoria())
        }
    }

    @Test
    fun `deve atualizar chave quando tipo for aleatorio`() {
        val valorAntigo = UUID.randomUUID().toString()
        val valorNovo = UUID.randomUUID().toString()
        val chavePix = novaChave(
            tipo = TipoChave.ALEATORIA,
            chave = valorAntigo
        )
        chavePix.atualizaChave(valorNovo)
        with(chavePix) {
            assertFalse(chavePix.chave.equals(valorAntigo))
            assertTrue(chavePix.chave.equals(valorNovo))
        }
    }

    @Test
    fun `nao deve atualizar chave quando tipo nao for aleatorio`() {
        val valorAntigo = UUID.randomUUID().toString()
        val valorNovo = UUID.randomUUID().toString()

        TIPOS_DE_CHAVE_EXCETO_ALEATORIO.forEach { tipoChave ->
            val chavePix = novaChave(
                tipo = tipoChave,
                chave = valorAntigo
            )
            chavePix.atualizaChave(valorNovo)
            with(chavePix) {
                assertTrue(chavePix.chave.equals(valorAntigo))
                assertFalse(chavePix.chave.equals(valorNovo))
            }
        }
    }

    fun novaChave(
        tipo: TipoChave,
        clientId: UUID = UUID.randomUUID(),
        chave: String = UUID.randomUUID().toString(),
    ): ChavePix {
        return ChavePix(
            clienteId = clientId,
            tipoChave = tipo,
            chave = chave,
            tipoConta = TipoConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                tipo = TipoConta.CONTA_CORRENTE.toString(),
                instituicao = ContaAssociada.Instituicao(
                    nomeInstituicao = "INSTITUICAO TESTE",
                    ispb = "60701190"
                ),
                agencia = "0001",
                numero = "291900",
                titular = ContaAssociada.Titular(
                    titularId = UUID.fromString("2ac09233-21b2-4276-84fb-d83dbd9f8bab"),
                    nomeTitular = "Titular Teste",
                    cpf = "83082363083"
                )
            )
        )
    }
}