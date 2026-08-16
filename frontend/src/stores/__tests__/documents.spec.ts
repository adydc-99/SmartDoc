import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { runUploadQueue, filtersFromQuery, filtersToQuery, useDocumentsStore } from '../documents'

const libraryApi=vi.hoisted(()=>({
  listFolders:vi.fn(),listTags:vi.fn(),listLibraryDocuments:vi.fn(),uploadDocument:vi.fn(),
  organizeDocument:vi.fn(),getDeleteImpact:vi.fn(),removeDocument:vi.fn(),
}))
vi.mock('../../api/library',()=>libraryApi)

const backendError=(message:string)=>({isAxiosError:true,message:'Request failed with status code 400',response:{data:{message}}})

describe('document workspace state', () => {
  beforeEach(()=>{setActivePinia(createPinia());vi.clearAllMocks()})
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

  it('shows the safe backend message when the document list fails',async()=>{
    libraryApi.listLibraryDocuments.mockRejectedValue(backendError('Unsupported library sort'))
    const store=useDocumentsStore()

    await store.loadDocuments()

    expect(store.error).toBe('Unsupported library sort')
  })

  it('shows the safe backend message when an upload fails',async()=>{
    libraryApi.uploadDocument.mockRejectedValue(backendError('PDF 不能超过 20 MB'))
    libraryApi.listLibraryDocuments.mockResolvedValue([])
    const store=useDocumentsStore()

    await store.uploadFiles([new File(['pdf'],'large.pdf',{type:'application/pdf'})])

    expect(store.uploads[0]).toMatchObject({state:'failure',error:'PDF 不能超过 20 MB'})
  })
})
