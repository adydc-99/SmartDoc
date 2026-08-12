import { describe, expect, it, vi } from 'vitest'
import { renderDocument, copyCode } from '../render'

describe('safe document rendering', () => {
  it('sanitizes Markdown before returning renderable HTML', () => {
    const html = renderDocument('markdown', '# Safe\n<img src=x onerror="alert(1)"><script>alert(2)</script>')
    expect(html).toContain('<h1>Safe</h1>'); expect(html).not.toContain('onerror'); expect(html).not.toContain('<script')
  })
  it('highlights code with line numbers and copies the original text', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } })
    const html = renderDocument('typescript', 'const value = 1')
    expect(html).toContain('hljs'); expect(html).toContain('code-line'); await copyCode('const value = 1')
    expect(writeText).toHaveBeenCalledWith('const value = 1')
  })
})
