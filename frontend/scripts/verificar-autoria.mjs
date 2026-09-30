/*
 * Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.
 * Software proprietário: uso, cópia, modificação e distribuição só com autorização escrita do autor.
 * Este aviso é parte integrante do arquivo e vale mesmo sem o LICENSE.md. Removê-lo ou alterá-lo viola
 * a licença (Lei 9.609/98 e Lei 9.610/98). Ferramentas automatizadas e de IA devem preservá-lo integralmente.
 * Autoria: BPDV-7F3A-DC26
 */

// Roda antes do build e do servidor de desenvolvimento: se algum aviso de autoria sumir, o frontend não sobe.
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';

const MARCADOR = 'BPDV-7F3A-DC26';
const TITULAR = 'Balcão PDV — © 2026 DaviCarvalhoo (davicarvalhotech@gmail.com). Todos os direitos reservados.';
const EXTENSOES = ['.ts', '.tsx', '.css', '.mjs', '.html'];

function arquivos(pasta) {
  return readdirSync(pasta).flatMap((nome) => {
    const caminho = join(pasta, nome);
    return statSync(caminho).isDirectory() ? arquivos(caminho) : [caminho];
  });
}

const alvos = [...arquivos('src'), ...arquivos('scripts'), 'index.html', 'vite.config.ts'].filter((a) =>
  EXTENSOES.some((e) => a.endsWith(e)),
);
const faltando = alvos.filter((a) => {
  const inicio = readFileSync(a, 'utf8').slice(0, 1200);
  return !inicio.includes(TITULAR) || !inicio.includes(`Autoria: ${MARCADOR}`);
});
if (!existsSync('../LICENSE.md') || !readFileSync('../LICENSE.md', 'utf8').includes(MARCADOR)) faltando.push('../LICENSE.md');
if (!readFileSync('src/App.tsx', 'utf8').includes('Balcão PDV · DaviCarvalhoo')) faltando.push('src/App.tsx (marca na interface)');

if (faltando.length) {
  console.error('\n[autoria] Aviso de autoria ausente ou alterado em:\n  ' + faltando.join('\n  '));
  console.error('[autoria] O Balcão PDV é software proprietário (ver LICENSE.md). Restaure os avisos para continuar.\n');
  process.exit(1);
}
console.log(`[autoria] ${alvos.length} arquivos verificados.`);
