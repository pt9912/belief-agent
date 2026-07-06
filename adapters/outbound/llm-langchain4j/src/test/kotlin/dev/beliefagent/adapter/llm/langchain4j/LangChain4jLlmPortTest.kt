package dev.beliefagent.adapter.llm.langchain4j

import dev.beliefagent.domain.belief.Beobachtung
import dev.beliefagent.domain.belief.BeliefState
import dev.beliefagent.domain.belief.Evidenz
import dev.beliefagent.domain.belief.Hypothese
import dev.beliefagent.domain.belief.HypotheseId
import dev.beliefagent.domain.belief.Quelle
import dev.beliefagent.domain.belief.Resthypothese
import dev.beliefagent.domain.belief.Zeitstempel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LangChain4jLlmPortTest {

    private fun prior() = BeliefState.of(
        listOf(
            Hypothese(HypotheseId("regression"), 0.4),
            Hypothese(HypotheseId("flaky"), 0.4),
        ),
        Resthypothese(0.2),
    )

    private fun beobachtung() = Beobachtung(
        Quelle.LOG,
        Zeitstempel(7L),
        Evidenz("regression im Zahlungsmodul wahrscheinlich"),
    )

    @Test
    fun fragt_langchain4j_runner_mit_beobachtung_und_hypothesen() {
        var prompt = ""
        val port = LangChain4jLlmPort(
            chat = LangChain4jChatRunner {
                prompt = it
                """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":0.2}"""
            },
        )

        val likelihoods = port.likelihoods(beobachtung(), prior())

        assertTrue(prompt.contains("regression im Zahlungsmodul wahrscheinlich"))
        assertTrue(prompt.contains("regression"))
        assertTrue(prompt.contains("flaky"))
        assertEquals(0.85, likelihoods.proHypothese[HypotheseId("regression")])
        assertEquals(0.15, likelihoods.proHypothese[HypotheseId("flaky")])
        assertEquals(0.2, likelihoods.resthypothese)
    }

    @Test
    fun weist_unbekannte_hypothese_zurueck() {
        val port = LangChain4jLlmPort(
            chat = LangChain4jChatRunner {
                """{"proHypothese":{"regression":0.85,"flaky":0.15,"anderes":0.1},"resthypothese":0.2}"""
            },
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            port.likelihoods(beobachtung(), prior())
        }

        assertTrue(fehler.message!!.contains("unbekannt=[anderes]"))
    }

    @Test
    fun weist_negative_werte_zurueck() {
        val port = LangChain4jLlmPort(
            chat = LangChain4jChatRunner {
                """{"proHypothese":{"regression":0.85,"flaky":-0.15},"resthypothese":0.2}"""
            },
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            port.likelihoods(beobachtung(), prior())
        }

        assertTrue(fehler.message!!.contains("flaky"))
    }

    @Test
    fun weist_fehlende_hypothese_zurueck() {
        val port = LangChain4jLlmPort(
            chat = LangChain4jChatRunner {
                """{"proHypothese":{"regression":0.85},"resthypothese":0.2}"""
            },
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            port.likelihoods(beobachtung(), prior())
        }

        assertTrue(fehler.message!!.contains("fehlend=[flaky]"))
    }

    @Test
    fun weist_negative_resthypothese_zurueck() {
        val port = LangChain4jLlmPort(
            chat = LangChain4jChatRunner {
                """{"proHypothese":{"regression":0.85,"flaky":0.15},"resthypothese":-0.2}"""
            },
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            port.likelihoods(beobachtung(), prior())
        }

        assertTrue(fehler.message!!.contains("Resthypothesen-Likelihood"))
    }

    @Test
    fun parser_verlangt_striktes_json_objekt() {
        val parser = StrictLangChain4jLikelihoodParser()

        assertFailsWith<IllegalArgumentException> {
            parser.parse("```json\n{\"proHypothese\":{},\"resthypothese\":0.1}\n```")
        }
    }

    @Test
    fun parser_verlangt_exakte_top_level_felder() {
        val parser = StrictLangChain4jLikelihoodParser()

        val fehler = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"proHypothese":{},"resthypothese":0.1,"aktion":"none"}""")
        }

        assertTrue(fehler.message!!.contains("exakt"))
    }

    @Test
    fun parser_verlangt_pro_hypothese_objekt() {
        val parser = StrictLangChain4jLikelihoodParser()

        val fehler = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"proHypothese":[],"resthypothese":0.1}""")
        }

        assertTrue(fehler.message!!.contains("proHypothese"))
    }

    @Test
    fun parser_verlangt_numerische_likelihoods() {
        val parser = StrictLangChain4jLikelihoodParser()

        val fehler = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"proHypothese":{"regression":"hoch"},"resthypothese":0.1}""")
        }

        assertTrue(fehler.message!!.contains("regression"))
    }

    @Test
    fun parser_verbietet_doppelte_hypothesen_ids() {
        val parser = StrictLangChain4jLikelihoodParser()

        val fehler = assertFailsWith<IllegalArgumentException> {
            parser.parse("""{"proHypothese":{"regression":0.85,"regression":0.75},"resthypothese":0.2}""")
        }

        assertTrue(fehler.message!!.contains("doppelte"))
    }
}
