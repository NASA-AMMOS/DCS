/*
 * Copyright 2022, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology
 * Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. By accepting
 * this software, the user agrees to comply with all applicable U.S.
 * export laws and regulations. User has the responsibility to obtain
 * export licenses, or other export authority as may be required before
 * exporting such information to foreign countries or providing access to
 * foreign persons.
 */
/**
 * @author panjames
 */
import React, {useEffect, useMemo, useState} from "react";
import {useFilters, useRowSelect, useTable} from "react-table";
import {
    Box,
    FormControl,
    IconButton,
    MenuItem,
    Paper,
    Popover,
    Select,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField
} from "@mui/material";
import {hexAuthLookup, hexEncLookup, hexToInt, serviceTypeLookupStr, stateLookupInt} from "./utilities";
import {Cancel, FilterAltOutlined} from "@mui/icons-material";
import SaModalForm from "./SaModalForm.jsx";
import {useSnackbar} from "notistack";
import {useLocalStorageJson} from "./useLocalStorage";
import SaTableAppBar from "./SaTableAppBar.jsx";
import {listSa} from "./api";

/**
 * SA Table
 * @param props
 * @returns {JSX.Element}
 * @constructor
 */
export default function SaTable(props) {

    /**
     * Snackbar for notifications
     */
    const {enqueueSnackbar} = useSnackbar()

    /**
     * Refresh table data
     */
    const refreshTable = () => {
        listSa(props.type, (result) => {
            const data = result.data
            setData(data)
        }, (err) => {
            let msg
            if (err.response) {
                msg = err.response.data.messages
            }
            enqueueSnackbar('Error retrieving from the SADB: ' + msg, {
                variant: 'error',
                preventDuplicate: true
            })
            console.log(err)
        })
    }

    /**
     * Table data
     */
    const [data, setData] = useState([])

    useEffect(() => {
        (refreshTable)()
        // eslint-disable-next-line react-hooks/exhaustive-deps -- intentional mount-only fetch
    }, [])

    /**
     * Default table column for react-table
     * @type {{Filter: function({column: {filterValue: *, preFilteredRows: *, setFilter: *}}): *}}
     */
    const defaultColumn = useMemo(() => ({
        Filter: DefaultColumnFilter
    }), [])

    /**
     * All columns
     * @type {[{Header: string, accessor: string},{Header: string, accessor: string},{Header: string, accessor: string},{Header: string, accessor: string},{Header: string, accessor: string},null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null]}
     */
    const columns = useMemo(() => [{
        Header: "SPI", accessor: "spi"
    }, {
        Header: "SCID", accessor: "scid"
    }, {
        Header: "VCID", accessor: "vcid"
    }, {
        Header: "TFVN", accessor: "tfvn"
    }, {
        Header: "MAPID", accessor: "mapid"
    }, {
        Header: "State",
        accessor: "saState",
        Cell: ({value}) => stateLookupInt(value), Filter: SelectColumnFilter,
    }, {
        Header: "Service Type",
        accessor: "serviceType",
        Filter: SelectColumnFilter, filter: 'includes'
    }, {
        Header: "EKID", accessor: "ekid"
    }, {
        Header: "AKID", accessor: "akid"
    }, {
        Header: "ECS",
        accessor: "ecs",
        Cell: ({value}) => hexEncLookup(value).name,
        Filter: SelectColumnFilter,
        filter: 'exact'
    }, {
        Header: "ACS",
        accessor: "acs",
        Cell: ({value}) => hexAuthLookup(value).name,
        Filter: SelectColumnFilter,
        filter: 'exact'
    }, {
        Header: "SHIVF Length", accessor: "shivfLen"
    }, {
        Header: "SHSNF Length", accessor: "shsnfLen"
    }, {
        Header: "SHPLF Length", accessor: "shplfLen"
    }, {
        Header: "STMACF Length", accessor: "stmacfLen"
    }, {
        Header: "IV Length", accessor: "ivLen"
    }, {
        Header: "IV", accessor: "iv"
    }, {
        Header: "ABM Length", accessor: "abmLen"
    }, {
        Header: "ABM", accessor: "abm"
    }, {
        Header: "ARSN Length", accessor: "arsnLen"
    }, {
        Header: "ARSN", accessor: "arsn"
    }, {
        Header: "ARSNW", accessor: "arsnw"
    }], [])

    /**
     * Default hidden columns
     * @type {string[]}
     */
    const defaultHiddenCols = [
        "shivfLen",
        "shsnfLen",
        "shplfLen",
        "stmacfLen",
        "ecsLen",
        "acsLen",
        "ivLen",
        "iv",
        "abmLen",
        "abm",
        "arsnLen",
        "arsn",
        "arsnw"]

    /**
     * Hidden column state
     */
    const [hiddenCols, setHiddenCols] = useLocalStorageJson("hiddenCols", defaultHiddenCols)

    const {
        getTableProps,
        getTableBodyProps,
        headerGroups,
        rows,
        prepareRow,
        selectedFlatRows,
        allColumns,
        toggleHideColumn,
        setHiddenColumns
    } = useTable({
            columns,
            data,
            defaultColumn,
            initialState: {
                hiddenColumns: hiddenCols
            }
        },
        useFilters,
        useRowSelect,
        hooks => {
            hooks.visibleColumns.push(columns => [
                {
                    id: 'selection',
                    Header: ({getToggleAllRowsSelectedProps}) => (
                        <div>
                            <IndeterminateCheckBox {...getToggleAllRowsSelectedProps()} />
                        </div>
                    ),
                    Cell: ({row}) => (
                        <div>
                            <IndeterminateCheckBox {...row.getToggleRowSelectedProps()} />
                        </div>
                    ),
                },
                ...columns,
            ])
        })

    /**
     * Reset default hidden columns
     */
    const resetDefaultCols = () => {
        setHiddenCols(defaultHiddenCols)
        setHiddenColumns(defaultHiddenCols)
    }

    /**
     * Hide a column
     * @param id column id
     * @param val boolean
     */
    const hideColumn = (id, val) => {
        toggleHideColumn(id, val)
        const cols = structuredClone(hiddenCols)
        if (val) {
            const idx = cols.indexOf(id)
            if (idx !== -1) {
                cols.splice(idx, 1)
            }
        } else {
            cols.push(id)
        }
        setHiddenCols(cols)
    }

    /**
     * Create/update modal stuff
     */
    const [rowOpen, setRowOpen] = useState(false)
    const [index, setIndex] = useState(0)
    const [isUpdate, setIsUpdate] = useState(false)

    /**
     * Handle create/update modal opening
     * @param e event
     * @param item row item
     * @param isUpdate is this an update or create
     */
    const handleRowOpen = (e, item, isUpdate) => {
        if (e && e.target instanceof HTMLInputElement) {
            return
        }
        setIndex(item)
        setIsUpdate(isUpdate)
        setRowOpen(true)
    }

    /**
     * Handle create/update modal close
     */
    const handleRowClose = () => {
        setRowOpen(false)
    }

    /**
     * Popovers
     */
    const [anchorEl, setAnchorEl] = useState(null)
    const [anchorElId, setAnchorElId] = useState(null)

    /**
     * Handle popover open clicks
     * @param e event
     * @param id popover id
     */
    const handlePopoverClick = (e, id) => {
        setAnchorEl(e.target)
        setAnchorElId(id)
    }

    /**
     * Handle popover close
     */
    const handlePopoverClose = () => {
        setAnchorEl(null)
        setAnchorElId(null)
    }

    return (<div>
        {rowOpen ? (
            <SaModalForm type={props.type} rowOpen={rowOpen} onClose={handleRowClose} data={data[index]}
                         refresh={refreshTable}
                         isUpdate={isUpdate}/>
        ) : (
            <div/>
        )}
        <Paper sx={{width: '100%', overflow: 'hidden'}}>
            <SaTableAppBar
                resetDefaultCols={resetDefaultCols}
                allColumns={allColumns}
                hideColumn={hideColumn}
                selectedFlatRows={selectedFlatRows}
                refreshTable={refreshTable}
                enqueueSnackbar={enqueueSnackbar}
                handleRowOpen={handleRowOpen}
                type={props.type}
            />
            <TableContainer sx={{maxHeight: 1024}}>
                <Table {...getTableProps()} stickyHeader sx={{minWidth: 650, ...getTableProps()}}
                       aria-label={"sticky table"}>
                    <TableHead key={"tablehead"}>
                        {headerGroups.map((headerGroup) => (
                            <TableRow key={"header"} {...headerGroup.getHeaderGroupProps()}>
                                {headerGroup.headers.map((column) => (<TableCell
                                    key={column.id} {...column.getHeaderProps()}>
                                    <Box sx={{
                                        whiteSpace: "nowrap",
                                        minWidth: "auto",
                                        textAlign: "center"
                                    }}
                                    >
                                        {column.render("Header")}
                                        {column.id !== 'selection' ? (
                                            <IconButton aria-describedby={anchorElId}
                                                        onClick={(e) => handlePopoverClick(e, column.id)}
                                                        color={column.filterValue ? "secondary" : "primary"}><FilterAltOutlined/></IconButton>
                                        ) : ''}
                                        {column.filterValue ? (
                                            <IconButton aria-describedby={anchorElId}
                                                        onClick={() => column.setFilter()}><Cancel/></IconButton>
                                        ) : ''}
                                    </Box>
                                    <Popover
                                        open={anchorElId === column.id}
                                        id={column.id}
                                        anchorEl={anchorEl}
                                        onClose={handlePopoverClose}
                                        anchorOrigin={{
                                            vertical: 'bottom', horizontal: 'left'
                                        }}
                                    >
                                        <div style={{padding: '16px'}}>
                                            Filter {column.render("Header")}<br/>{column.canFilter ? column.render('Filter') : null}
                                        </div>
                                    </Popover>
                                </TableCell>))}
                            </TableRow>))}
                    </TableHead>
                    <TableBody {...getTableBodyProps()}>
                        {rows.map((row) => {
                            prepareRow(row)
                            return (<TableRow key={row.original.spi + "-" + row.original.scid} {...row.getRowProps()}
                                              onClick={(e) => handleRowOpen(e, row.index, true)} hover
                                              role={"checkbox"}
                                              sx={{...row.getRowProps()}}
                            >
                                {row.cells.map((cell) => {
                                    return <TableCell
                                        key={row.original.spi + "-" + row.original.scid + "-" + cell.column.id} {...cell.getCellProps()}>{cell.render("Cell")}</TableCell>
                                })}
                            </TableRow>)
                        })}
                    </TableBody>
                </Table>
            </TableContainer>
        </Paper>
    </div>)
}

/**
 * Allows checkboxes to interact with table/row state. From react-table examples.
 * @type {React.ForwardRefExoticComponent<React.PropsWithoutRef<{readonly indeterminate?: *}> & React.RefAttributes<unknown>>}
 */
const IndeterminateCheckBox = React.forwardRef(
    ({indeterminate, ...rest}, ref) => {
        const defaultRef = React.useRef()
        const resolvedRef = ref || defaultRef
        useEffect(() => {
            resolvedRef.current.indeterminate = indeterminate
        }, [resolvedRef, indeterminate])
        return (
            <div>
                <input type={"checkbox"} ref={resolvedRef} {...rest} />
            </div>
        )
    }
)

/**
 * Default filter for columns. From react-table examples.
 * @param filterValue
 * @param preFilteredRows
 * @param setFilter
 * @returns {JSX.Element}
 * @constructor
 */
function DefaultColumnFilter({
                                 column: {filterValue, preFilteredRows, setFilter},
                             }) {
    const count = preFilteredRows.length
    return (<TextField
        value={filterValue || ''}
        onChange={e => {
            setFilter(e.target.value || undefined)
        }}
        placeholder={`Filter ${count} items`}
        autoFocus
    />)
}

/**
 * Selection column filter. From react-table examples.
 * @param filterValue
 * @param setFilter
 * @param preFilteredRows
 * @param id
 * @returns {JSX.Element}
 * @constructor
 */
function SelectColumnFilter({
                                column: {filterValue, setFilter, preFilteredRows, id},
                            }) {
    const options = React.useMemo(() => {
        const options = new Set()
        preFilteredRows.forEach(row => {
            options.add(row.values[id])
        })
        return [...options.values()]
    }, [id, preFilteredRows])

    return (<FormControl fullWidth>
        <Select
            value={filterValue == null ? 100 : filterValue}
            onChange={e => {
                let filterValue = e.target.value || undefined
                if (filterValue === 100) {
                    filterValue = undefined
                }
                setFilter(filterValue)
            }}
        >
            <MenuItem value={100}>All</MenuItem>
            {options.map((option, i) => {
                let itemValue
                switch (id) {
                    case 'saState':
                        itemValue = stateLookupInt(option)
                        break
                    case 'ecs':
                        itemValue = hexEncLookup(option).value
                        break
                    case 'acs':
                        itemValue = hexAuthLookup(option).value
                        break
                    default:
                        itemValue = option
                }

                return (<MenuItem key={i} value={option}>
                    {itemValue}
                </MenuItem>)
            }).sort((a, b) => {
                if (id === 'saState') {
                    return a.props.value - b.props.value
                } else if (id === 'ecs' || id === 'acs') {
                    return hexToInt(a.props.value) - hexToInt(b.props.value)
                } else {
                    return serviceTypeLookupStr(a.props.value) - serviceTypeLookupStr(b.props.value)
                }
            })}
        </Select>
    </FormControl>)
}
