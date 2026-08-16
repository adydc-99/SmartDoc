import {describe,expect,it,vi} from 'vitest'
import {listLibraryDocuments} from '../library'

const request=vi.hoisted(()=>({get:vi.fn()}))
vi.mock('../../api',()=>({api:request}))

describe('library backend contract',()=>{
  it('normalizes UI sort values only at the API boundary',async()=>{
    request.get.mockResolvedValue({data:[]})
    const filters={sort:'updated-desc',favorite:true}

    await listLibraryDocuments(filters)

    expect(request.get).toHaveBeenCalledWith('/library/documents',{params:{sort:'updated,desc',favorite:true}})
    expect(filters.sort).toBe('updated-desc')
  })
})
