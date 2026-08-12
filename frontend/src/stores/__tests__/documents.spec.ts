import { describe, expect, it, vi } from 'vitest'
import { runUploadQueue, filtersFromQuery, filtersToQuery } from '../documents'

describe('document workspace state', () => {
  it('mirrors supported filters to and from URL query values', () => {
    const filters = filtersFromQuery({ query: 'JVM', type: 'PDF', favorite: 'true', sort: 'name-asc', tagId: '7' })
    expect(filters).toMatchObject({ query: 'JVM', type: 'PDF', favorite: true, sort: 'name-asc', tagId: 7 })
    expect(filtersToQuery(filters)).toMatchObject({ query: 'JVM', type: 'PDF', favorite: 'true', sort: 'name-asc', tagId: '7' })
  })

  it('uploads with concurrency exactly two', async () => {
    let active = 0
    let peak = 0
    const release: Array<() => void> = []
    const uploader = vi.fn(async () => {
      active += 1
      peak = Math.max(peak, active)
      await new Promise<void>((resolve) => release.push(resolve))
      active -= 1
    })
    const files = [1, 2, 3, 4].map((index) => new File(['x'], `${index}.txt`, { type: 'text/plain' }))
    const pending = runUploadQueue(files, uploader)
    await Promise.resolve()
    expect(uploader).toHaveBeenCalledTimes(2)
    release.shift()?.()
    release.shift()?.()
    await Promise.resolve()
    await Promise.resolve()
    expect(uploader).toHaveBeenCalledTimes(4)
    release.splice(0).forEach((done) => done())
    await pending
    expect(peak).toBe(2)
  })
})
